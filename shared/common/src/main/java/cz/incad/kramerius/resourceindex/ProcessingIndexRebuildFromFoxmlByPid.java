package cz.incad.kramerius.resourceindex;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.qbizm.kramerius.imp.jaxb.DatastreamType;
import com.qbizm.kramerius.imp.jaxb.DigitalObject;
import cz.incad.kramerius.fedora.RepoModule;
import cz.incad.kramerius.fedora.om.RepositoryException;
import cz.incad.kramerius.fedora.om.impl.AkubraObject;
import cz.incad.kramerius.fedora.om.impl.AkubraUtils;
import cz.incad.kramerius.fedora.om.impl.RELSEXTSPARQLBuilder;
import cz.incad.kramerius.fedora.om.impl.RELSEXTSPARQLBuilderImpl;
import cz.incad.kramerius.processes.new_api.IndexationScheduler;
import cz.incad.kramerius.processes.starter.ProcessStarter;
import cz.incad.kramerius.solr.SolrModule;
import cz.incad.kramerius.statistics.NullStatisticsModule;
import cz.incad.kramerius.utils.FedoraUtils;
import cz.incad.kramerius.utils.conf.KConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.xml.sax.SAXException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessingIndexRebuildFromFoxmlByPid {
    public static final Logger LOGGER = Logger.getLogger(ProcessingIndexRebuildFromFoxmlByPid.class.getName());

    private final Unmarshaller unmarshaller;
    private final ProcessingIndexFeeder feeder;

    private ProcessingIndexRebuildFromFoxmlByPid() {
        this.unmarshaller = initUnmarshaller();
        Injector injector = Guice.createInjector(new SolrModule(), new ResourceIndexModule(), new RepoModule(), new NullStatisticsModule());
        this.feeder = injector.getInstance(ProcessingIndexFeeder.class);
    }

    public static void main(String[] args) throws IOException, SolrServerException, RepositoryException, FcrepoOperationFailedException {
        //args
        /*LOGGER.info("args: " + Arrays.asList(args));
        for (String arg : args) {
            System.out.println(arg);
        }*/
        if (args.length < 5) { //at least 5 args ar necessary:
            // A. credentials for scheduling another process (in the same batch) after this process has finished - 4 args
            // B. process-specific args - 1 args
            throw new RuntimeException("Not enough arguments.");
        }
        int argsIndex = 0;
        IndexationScheduler.ProcessCredentials processCredentials = new IndexationScheduler.ProcessCredentials();
        //token for keeping possible following processes in same batch
        processCredentials.authToken = args[argsIndex++]; //auth token always first, but still suboptimal solution, best would be if it was outside the scope of this as if ProcessHelper.scheduleProcess() similarly to changing name (ProcessStarter)
        //Kramerius
        processCredentials.krameriusApiAuthClient = args[argsIndex++];
        processCredentials.krameriusApiAuthUid = args[argsIndex++];
        processCredentials.krameriusApiAuthAccessToken = args[argsIndex++];
        //process-specific args
        String pid = args[argsIndex++];

        ProcessStarter.updateName(String.format("Aktualizace Processing indexu z FOXML objektu %s", pid));
        new ProcessingIndexRebuildFromFoxmlByPid().rebuildProcessingIndexFromFoxml(pid);
    }

    private Unmarshaller initUnmarshaller() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DigitalObject.class);
            return jaxbContext.createUnmarshaller();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Cannot init JAXB", e);
            throw new RuntimeException(e);
        }
    }

    private void rebuildProcessingIndexFromFoxml(String pid) throws IOException {
        LOGGER.log(Level.INFO, "Updating processing index from FOXML of " + pid);
        File foxmlFile = findFoxmlFile(pid);
        LOGGER.log(Level.INFO, "FOXML file: " + foxmlFile.getAbsolutePath());
        if (!foxmlFile.exists()) {
            throw new IOException("File doesn't exist: " + foxmlFile.getAbsolutePath());
        }
        if (!foxmlFile.canRead()) {
            throw new IOException("File can't be read: " + foxmlFile.getAbsolutePath());
        }
        try {
            FileInputStream inputStream = new FileInputStream(foxmlFile);
            DigitalObject digitalObject = createDigitalObject(inputStream);
            feeder.deleteByPid(pid); //smazat vsechny existujici vazby z objektu, ALE netyka se tech, co na objekt vedou (ty ted neprebudovavame)
            rebuildProcessingIndex(feeder, digitalObject);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error processing file: " + foxmlFile.getAbsolutePath(), ex);
        }
    }

    private File findFoxmlFile(String pid) {
        try {
            if (!pid.toLowerCase().startsWith("uuid:")) { //this is already checked at API endpoint level, here it's just to make sure if this class was to be called from somewhere else
                throw new IllegalArgumentException("invalid pid format");
            }
            String objectId = "info:fedora/" + pid.toLowerCase(); //e.g. info:fedora/uuid:912509d3-2764-4be5-9e0a-366cbacabfef
            //System.out.println(objectId);
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(objectId.getBytes("UTF-8"));
            String objectIdHash = DatatypeConverter.printHexBinary(md.digest()); //e.g. 7C2BDE15DDDFA29123823CB7A86BFD86
            //System.out.println(objectIdHash);
            String objectPattern = KConfiguration.getInstance().getProperty("objectStore.pattern"); //e.g. ##/##/##
            //System.out.println(objectPattern);
            String pathSegementsFromPid = PathSegmentExtractor.extractPathSegements(objectIdHash.toLowerCase(), objectPattern); //e.g. 7c/2b/de
            //System.out.println(pathSegementsFromPid);
            String foxmlPath = pathSegementsFromPid + "/info%3Afedora%2Fuuid%3A" + pid.substring("uuid:".length()); //e.g. 7c/2b/de/info%3Afedora%2Fuuid%3A912509d3-2764-4be5-9e0a-366cbacabfef
            //System.out.println(foxmlPath);
            File objectStoreRoot = new File(KConfiguration.getInstance().getProperty("objectStore.path")); //e.g. /home/tomcat/kramerius-akubra/akubra-data/objectStore
            return new File(objectStoreRoot, foxmlPath); //e.g. /home/tomcat/kramerius-akubra/akubra-data/objectStore/
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private DigitalObject createDigitalObject(InputStream inputStream) {
        try {
            return (DigitalObject) unmarshaller.unmarshal(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void rebuildProcessingIndex(ProcessingIndexFeeder feeder, DigitalObject digitalObject) throws RepositoryException {
        try {
            List<DatastreamType> datastreamList = digitalObject.getDatastream();
            for (DatastreamType datastreamType : datastreamList) {
                if (FedoraUtils.RELS_EXT_STREAM.equals(datastreamType.getID())) {
                    InputStream streamContent = AkubraUtils.getStreamContent(AkubraUtils.getLastStreamVersion(datastreamType), null);
                    AkubraObject akubraObject = new AkubraObject(null, digitalObject.getPID(), digitalObject, feeder);
                    rebuildProcessingIndexImpl(akubraObject, streamContent);
                }
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    private void rebuildProcessingIndexImpl(AkubraObject akubraObject, InputStream content) throws RepositoryException {
        try {
            String s = IOUtils.toString(content, "UTF-8");
            RELSEXTSPARQLBuilder sparqlBuilder = new RELSEXTSPARQLBuilderImpl();
            sparqlBuilder.sparqlProps(s.trim(), (object, localName) -> {
                akubraObject.processRELSEXTRelationAndFeedProcessingIndex(object, localName);
                return object;
            });
            LOGGER.info("Processed " + akubraObject.getPid());
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (SAXException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see org.fcrepo.server.storage.lowlevel.akubra.HashPathIdMapper
     */
    public static class PathSegmentExtractor {
        public static String extractPathSegements(String string, String objectPattern) {
            if (!objectPattern.matches("#+(\\/#+)*")) {
                throw new RuntimeException(String.format("unsupported object pattern: %s", objectPattern));
            }
            if (objectPattern.replaceAll("\\/", "").length() > string.length()) {
                throw new RuntimeException(String.format("string too short for the pattern: %s, string: %s", objectPattern, string));
            }
            StringBuilder builder = new StringBuilder();
            String[] placeholders = objectPattern.split("\\/");
            int startingPosition = 0;
            for (int i = 0; i < placeholders.length; i++) {
                String placeholder = placeholders[i];
                builder.append(string, startingPosition, startingPosition + placeholder.length());
                startingPosition += placeholder.length();
                if (placeholders.length != 1 && i != placeholders.length - 1) {
                    builder.append('/');
                }
            }
            return builder.toString();
        }
    }
}