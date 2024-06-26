package cz.incad.kramerius;

import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import cz.incad.kramerius.processes.starter.ProcessStarter;
import cz.incad.kramerius.service.MigrateSolrIndexException;
import cz.incad.kramerius.services.MigrationUtils;
import cz.incad.kramerius.solr.SolrFieldsMapping;
import cz.incad.kramerius.utils.IOUtils;
import cz.incad.kramerius.utils.IterationUtils;
import cz.incad.kramerius.utils.XMLUtils;
import cz.incad.kramerius.utils.conf.KConfiguration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LicensesExport {

    public static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HHmmssZ");

    public static final Logger LOGGER = Logger.getLogger(LicensesExport.class.getName());

    public static final String EXPORT_DNNT_MODELS_KEY="export.licenses.models";
    public static final String EXPORT_LICENSES_FILE_KEY ="export.licenses.file";
    public static final String EXPORT_LICENSES_FOLDER_KEY ="export.licenses.directory";
    public static final String LICENSES_SOLR_EXPORT_KEY = "export.licenses.query";


    public static void main(String[] args) throws InterruptedException, BrokenBarrierException, SAXException, IOException, ParserConfigurationException, MigrateSolrIndexException {
        File csvFile = csvFile(args);
        //ProcessStarter.updateName("Licenses export   '"+csvFile.getAbsolutePath()+"'");

        String labelQuery = args.length > 0 ? "(licenses:"+args[0]+")" :  "(licenses:*)";

        //String reduced = Arrays.stream(KConfiguration.getInstance().getConfiguration().getStringArray(EXPORT_DNNT_MODELS_KEY)).map(it -> " fedora.model:" + it).collect(Collectors.joining(" OR "));
        String query = labelQuery +" AND NOT (model:page)";

        Client client = Client.create();
        String q = KConfiguration.getInstance().getConfiguration().getString(LICENSES_SOLR_EXPORT_KEY,query);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(csvFile), Charset.forName("UTF-8"));
        try (CSVPrinter printer = new CSVPrinter(outputStreamWriter, CSVFormat.DEFAULT.withHeader("pid","model","dctitle","labels"))) {
            IterationUtils.cursorIteration(client,KConfiguration.getInstance().getSolrSearchHost() ,  URLEncoder.encode(q,"UTF-8"),(em, i) -> {
                List<String> pp = IterationUtils.findAllPids(em);
                if (!pp.isEmpty()) {
                    Lists.partition(pp, 10).stream().forEach(it->{
                        try {
                            Element response = MigrationUtils.fetchDocuments(client, KConfiguration.getInstance().getSolrSearchHost(), it);
                            Element resultElem = XMLUtils.findElement(response, (elm) -> {
                                return elm.getNodeName().equals("result");
                            });
                            List<Element> docs = XMLUtils.getElements(resultElem, (elm) -> {
                                return elm.getNodeName().equals("doc");
                            });
                            docs.forEach(doc-> {
                                Element pid = XMLUtils.findElement(doc, (elm) -> {
                                    if (elm.getNodeName().equals("str") && elm.getAttribute("name").equals("pid")) {
                                        return true;
                                    } else return false;
                                });
                                Element dcTitle = XMLUtils.findElement(doc, (elm) -> {
                                    if (elm.getNodeName().equals("str") && elm.getAttribute("name").equals("title.search")) {
                                        return true;
                                    } else return false;
                                });
                                Element fedoraModel = XMLUtils.findElement(doc, (elm) -> {
                                    if (elm.getNodeName().equals("str") && elm.getAttribute("name").equals("model")) {
                                        return true;
                                    } else return false;
                                });

                                Element dnntLabels = XMLUtils.findElement(doc, (elm) -> {
                                    if (elm.getNodeName().equals("arr") && elm.getAttribute("name").equals("licenses")) {
                                        return true;
                                    } else return false;
                                });

                                List<String> labels = new ArrayList<>();
                                if (dnntLabels != null) {
                                    NodeList childNodes = dnntLabels.getChildNodes();
                                    for (int j = 0; j < childNodes.getLength(); j++) {
                                        labels.add(childNodes.item(j).getTextContent());
                                    }
                                }

                                try {
                                    printer.printRecord(pid.getTextContent(),fedoraModel.getTextContent(),dcTitle != null ? dcTitle.getTextContent() :"", labels.stream().collect(Collectors.joining(" ")));
                                } catch (IOException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(),e);
                                }
                            });
                        } catch (ParserConfigurationException |  SAXException | IOException | MigrateSolrIndexException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(),e);
                        }
                    });
                }
            }, ()->{});
        }

    }

    private static File csvFile(String[] args) {
        if (args.length > 0 ){
            return new File( exportDirectory(), KConfiguration.getInstance().getConfiguration().getString(EXPORT_LICENSES_FILE_KEY, "licenses-export-" + SIMPLE_DATE_FORMAT.format(new Date())+"_"+args[0] + ".csv"));
        } else {
            return new File( exportDirectory(), KConfiguration.getInstance().getConfiguration().getString(EXPORT_LICENSES_FILE_KEY, "licenses-export-" + SIMPLE_DATE_FORMAT.format(new Date()) + ".csv"));
        }
    }


    private  static File exportDirectory() {
        String exportRoot = KConfiguration.getInstance().getConfiguration().getString(EXPORT_LICENSES_FOLDER_KEY);
        return IOUtils.checkDirectory(exportRoot);
    }
}
