package oxy;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created by SBT-Funikov-YY on 05.11.2015.
 */
public class SyncDirectoryRoutes extends RouteBuilder {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String targetDirs[] = new String[] {"a\\", "b\\"};

    private String sourceDir = "source\\";

    private String fileName = "*.txt";

    private Map<String, String> dirIds = new HashMap<>(targetDirs.length);

    private Map<String, HashSet<String>> processedFiles = new HashMap<>();

    public SyncDirectoryRoutes(String sourceDir, String fileName, String[] targetDirs) {

        Assert.notNull(sourceDir);
        Assert.notNull(fileName);
        Assert.notEmpty(targetDirs);

        this.targetDirs = targetDirs;
        this.sourceDir = sourceDir;
        this.fileName = fileName;
    }

    public SyncDirectoryRoutes(CamelContext context, String sourceDir, String fileName, String[] targetDirs) {
        super(context);

        Assert.notNull(sourceDir);
        Assert.notNull(fileName);
        Assert.notEmpty(targetDirs);

        this.targetDirs = targetDirs;
        this.sourceDir = sourceDir;
        this.fileName = fileName;
    }

    @Override
    public void configure() throws Exception {

        List<String> targetConsumers = new ArrayList<>();

        for (String targetDir: targetDirs) {
            dirIds.put(targetDir, Integer.toHexString(targetDir.hashCode()));

            targetConsumers.add(setupRoute(sourceDir, targetDir));
        }

        String uri = MessageFormat.format("file:{0}?antInclude={1}&noop=true", sourceDir, fileName);


        addConsumers(
                from(uri).id(MessageFormat.format("filesync-{0}", sourceDir)).
                        multicast(),
                targetConsumers)
                .end();

    }

    private MulticastDefinition addConsumers(MulticastDefinition multicast, List<String> targetConsumers) {
        for (String consumer: targetConsumers) {
            multicast = multicast.to(consumer);
        }

        return multicast;
    }

    private String setupRoute(String sourceDir, final String targetDir) throws Exception {

        String sourceUri = MessageFormat.format("direct:{0}", targetDir);

        String targetUri = MessageFormat.format("file:{0}", targetDir);

        from(sourceUri).id(MessageFormat.format("filesync-{0}-{1}", sourceDir, targetDir)).to(targetUri).bean(removeIfAllDone(targetDir));

        return  sourceUri;
    }

    private Processor removeIfAllDone(final String targetDir) {
        return new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                String fileName = (String) exchange.getIn().getHeader(Exchange.FILE_NAME);
                String filePath = (String) exchange.getIn().getHeader("CamelFileAbsolutePath");

                synchronized (processedFiles) {
                    HashSet<String> fileProcessedBy = processedFiles.get(fileName);

                    if (fileProcessedBy == null) {
                        fileProcessedBy = new HashSet<>();

                        processedFiles.put(fileName, fileProcessedBy);
                    }

                    fileProcessedBy.add(targetDir);

                    boolean canRemove = true;
                    for (String targetDir : targetDirs) {
                        canRemove = fileProcessedBy.contains(targetDir);
                        if (!canRemove) {
                            logger.debug("file {} can't be removed because not yet processed by {}", fileName, targetDir);
                            break;
                        }
                    }

                    if (canRemove) {
                        FileUtil.deleteFile(new File(filePath));
                        logger.debug("file {} removed", fileName);
                    }
                }
            }
        };
    }
}
