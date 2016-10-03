/*
 * Copyright (C) 2012 Pavel Stastny
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package cz.incad.Kramerius.statistics.formatters.report.date;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.http.HttpServletResponse;

import cz.incad.Kramerius.statistics.formatters.report.StatisticsReportFormatter;
import cz.incad.Kramerius.statistics.formatters.report.author.AuthorXMLFormatter;
import cz.incad.Kramerius.statistics.formatters.utils.StringUtils;
import cz.incad.kramerius.statistics.impl.DateDurationReport;

/**
 * @author pavels
 *
 */
public class DateXMLFormatter {
//    implements StatisticsReportFormatter{
//}
//
//    static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(AuthorXMLFormatter.class.getName());
//    
//    private OutputStream os;
//
//    @Override
//    public String getMimeType() {
//        return XML_MIME_TYPE;
//    }
//
//    @Override
//    public String getFormat() {
//        return XML_FORMAT;
//    }
//
//    @Override
//    public void beforeProcess(HttpServletResponse response) throws IOException {
//        this.os = response.getOutputStream();
//        this.os.write("<records>\n".getBytes("UTF-8"));
//    }
//
//
//    @Override
//    public void afterProcess(HttpServletResponse response) throws IOException {
//        this.os.write("\n</records>".getBytes("UTF-8"));
//        this.os = null;
//    }
//
//
//    
//    
//    @Override
//    public void processReportRecord(Map<String, Object> record) {
//        try {
//
//            StringBuilder builder = new StringBuilder("<record>\n");
//            builder.append("\t<count>").append(record.get("count")).append("</count>\n");
//            builder.append("\t<pid>").append(StringUtils.nullify((String)record.get("pid"))).append("</pid>\n");
//            builder.append("\t<date>").append(StringUtils.nullify((String)record.get("date"))).append("</date>\n");
//            builder.append("\t<remote_ip_address>").append(StringUtils.nullify((String)record.get("remote_ip_address"))).append("</remote_ip_address>\n");
//            builder.append("\t<user>").append(StringUtils.nullify((String)record.get("user"))).append("</user>\n");
//            builder.append("\t<issued_date>").append(StringUtils.nullify((String)record.get("issued_date"))).append("</issued_date>\n");
//            builder.append("\t<rights>").append(StringUtils.nullify((String)record.get("rights"))).append("</rights>\n");
//            builder.append("\t<lang>").append(StringUtils.nullify((String)record.get("lang"))).append("</lang>\n");
//            builder.append("\t<title>").append(StringUtils.nullify((String)record.get("title"))).append("</title>\n");
//            builder.append("</record>\n");
//
//            this.os.write(builder.toString().getBytes("UTF-8"));
//        } catch (UnsupportedEncodingException e) {
//            LOGGER.log(Level.SEVERE,e.getMessage(),e);
//        } catch (IOException e) {
//            LOGGER.log(Level.SEVERE,e.getMessage(),e);
//        }
//       
//    }
//
//    @Override
//    public String getReportId() {
//        return DateDurationReport.REPORT_ID;
//    }
}
