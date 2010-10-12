package cz.incad.Kramerius;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.net.httpserver.HttpServer;

import cz.incad.Kramerius.backend.guice.GuiceServlet;
import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.security.SecurityException;
import cz.incad.kramerius.utils.FedoraUtils;
import cz.incad.kramerius.utils.XMLUtils;
import cz.incad.kramerius.utils.conf.KConfiguration;
import cz.incad.kramerius.utils.imgs.KrameriusImageSupport;
import cz.incad.kramerius.utils.imgs.KrameriusImageSupport.ScalingMethod;
import cz.incad.utils.SafeSimpleDateFormat;

public abstract class AbstractImageServlet extends GuiceServlet {

    protected static final DateFormat [] XSD_DATE_FORMATS =
    {
      new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'S'Z'"),
      new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
      new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'S"), 
      new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
      new SafeSimpleDateFormat("yyyy-MM-dd'Z'"),
      new SafeSimpleDateFormat("yyyy-MM-dd")
    };

    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	public static final java.util.logging.Logger LOGGER = java.util.logging.Logger
			.getLogger(AbstractImageServlet.class.getName());
	
	public static final String SCALE_PARAMETER = "scale";
	public static final String SCALED_HEIGHT_PARAMETER = "scaledHeight";
	public static final String SCALED_WIDTH_PARAMETER = "scaledWidth";
	public static final String OUTPUT_FORMAT_PARAMETER="outputFormat";
	
	@Inject
	protected transient KConfiguration configuration;

	@Inject
	@Named("securedFedoraAccess")
	protected transient FedoraAccess fedoraAccess;
	
	protected BufferedImage scale(BufferedImage img, Rectangle pageBounds, HttpServletRequest req) {
		String spercent = req.getParameter(SCALE_PARAMETER);
		String sheight = req.getParameter(SCALED_HEIGHT_PARAMETER);
		String swidth = req.getParameter(SCALED_WIDTH_PARAMETER);
		//System.out.println("REQUEST PARAMS: sheight:"+sheight+"swidth:"+swidth);
		if (spercent != null) {
			double percent = 1.0; {
				try {
					percent = Double.parseDouble(spercent);
				} catch (NumberFormatException e) {
					log(e.getMessage());
				}
			}
			return scaleByPercent(img, pageBounds, percent);
		} else if (sheight != null){
			int height = 200; {
				try {
					height = Integer.parseInt(sheight);
				} catch (NumberFormatException e) {
					log(e.getMessage());
				}
			}
			return scaleByHeight(img,pageBounds, height);
		} else if (swidth != null){
			int width = 200; {
				try {
					width = Integer.parseInt(swidth);
				} catch (NumberFormatException e) {
					log(e.getMessage());
				}
			}
			return scaleByWidth(img,pageBounds, width);
		}else return null;
	}
	
	protected BufferedImage scaleByHeight(BufferedImage img, Rectangle pageBounds, int height) {
		long start = System.currentTimeMillis();
		int nHeight = height;
		double div = (double)pageBounds.getHeight() / (double)nHeight;
		double nWidth = (double)pageBounds.getWidth() / div;
		BufferedImage scaledImage = KrameriusImageSupport.scale(img, (int)nWidth, nHeight, ScalingMethod.BILINEAR, false);
		long stop = System.currentTimeMillis();
		LOGGER.info("DEB - scale takes :"+(stop - start));
		return scaledImage;
	}

	protected BufferedImage scaleByWidth(BufferedImage img, Rectangle pageBounds, int width) {
		long start = System.currentTimeMillis();
		int nWidth = width;
		double div = (double)pageBounds.getWidth() / (double)nWidth;
		double nHeight = (double)pageBounds.getHeight() / div;
		BufferedImage scaledImage = KrameriusImageSupport.scale(img, nWidth,(int) nHeight,ScalingMethod.BILINEAR, false);
		long stop = System.currentTimeMillis();
		LOGGER.info("DEB - scale takes :"+(stop - start));
		return scaledImage;
	}

	
	protected BufferedImage rawThumbnailImage(String uuid, int page) throws XPathExpressionException, IOException, SecurityException {
		return KrameriusImageSupport.readImage(uuid, FedoraUtils.IMG_THUMB_STREAM, this.fedoraAccess,page);
	}
	
	protected BufferedImage rawFullImage(String uuid, HttpServletRequest request, int page) throws IOException, MalformedURLException, XPathExpressionException {
		return KrameriusImageSupport.readImage(uuid, FedoraUtils.IMG_FULL_STREAM, this.fedoraAccess, page);
	}
	
	protected void writeImage(HttpServletRequest req, HttpServletResponse resp, BufferedImage image, OutputFormats format) throws IOException {
		if ((format.equals(OutputFormats.JPEG)) || 
			(format.equals(OutputFormats.PNG))) {
			resp.setContentType(format.getMimeType());
			OutputStream os = resp.getOutputStream();
			KrameriusImageSupport.writeImageToStream(image, format.getJavaFormat(), os);
		} else throw new IllegalArgumentException("unsupported mimetype '"+format+"'");
	}

    protected void setDateHaders(String uuid, HttpServletResponse resp) throws IOException {
        Date lastModifiedDate = lastModified(uuid);
        Calendar instance = Calendar.getInstance();
        instance.roll(Calendar.YEAR, 1);
        resp.setDateHeader("Last Modified", lastModifiedDate.getTime());
        resp.setDateHeader("Last Fetched", System.currentTimeMillis());
        resp.setDateHeader("Expires", instance.getTime().getTime());
    }

    private Date lastModified(String uuid) throws IOException {
        Date date = null;
        Document fullProfile = fedoraAccess.getImageFULLProfile(uuid);
        Element elm = XMLUtils.findElement(fullProfile.getDocumentElement(), "dsCreateDate", null);
        if (elm != null) {
            String textContent = elm.getTextContent();
            for(DateFormat df:XSD_DATE_FORMATS) {
                try {
                    date = df.parse(textContent);
                    break;
                } catch (ParseException e) {
                    //
                }
            }
        }
        if (date == null) {
            date = new Date();
        }
        return date;
    }
    
    
    protected void setResponseCode(String uuid, HttpServletRequest request, HttpServletResponse response) throws IOException {
        long dateHeader = request.getDateHeader("If-Modified-Since");
        if (dateHeader != -1) {
            Date reqDate = new Date(dateHeader);
            Date lastModified = lastModified(uuid);
            if (lastModified.after(reqDate)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            }
        }
    }
	protected BufferedImage scaleByPercent(BufferedImage img, Rectangle pageBounds, double percent) {
		if ((percent <= 0.95) || (percent >= 1.15)) {
			int nWidth = (int) (pageBounds.getWidth() * percent);
			int nHeight = (int) (pageBounds.getHeight() * percent);
			BufferedImage scaledImage = KrameriusImageSupport.scale(img, nWidth, nHeight,ScalingMethod.BILINEAR, false);
			return scaledImage;
		} else return (BufferedImage) img;
	}

	public FedoraAccess getFedoraAccess() {
		return fedoraAccess;
	}

	public void setFedoraAccess(FedoraAccess fedoraAccess) {
		this.fedoraAccess = fedoraAccess;
	}

	
	public abstract ScalingMethod getScalingMethod();

	public abstract boolean turnOnIterateScaling();
	
//	KConfiguration config = KConfiguration.getInstance();
//	ScalingMethod method = ScalingMethod.valueOf(config.getProperty(
//			"scalingMethod", "BICUBIC_STEPPED"));

	
	public enum OutputFormats {
		JPEG("image/jpeg","jpg"),
		PNG("image/png","png"),

		VNDDJVU("image/vnd.djvu",null),
		XDJVU("image/x.djvu",null),
		DJVU("image/djvu",null),

		RAW(null,null);
		
		String mimeType;
		String javaFormat;

		private OutputFormats(String mimeType, String javaFormat) {
			this.mimeType = mimeType;
			this.javaFormat = javaFormat;
		}

		public String getMimeType() {
			return mimeType;
		}

		public String getJavaFormat() {
			return javaFormat;
		}
	}
	
	
 
}
