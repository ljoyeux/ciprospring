package fr.devlogic.encrypt.spring.impl;

import fr.devlogic.encrypt.util.EncryptException;
import fr.devlogic.encrypt.spring.EncryptedProperty;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class EncryptedPropertyAgent {
    private static final Logger log = LoggerFactory.getLogger(EncryptedPropertyAgent.class);

    private static List<String> allClasses = null;
    private static final Set<String> classesSet = new HashSet<>();

    public static void agentmain(String agentArgs, Instrumentation inst) {
        List<String> classNames =
                Arrays.stream(inst.getAllLoadedClasses())
                        .filter(c -> c.getClassLoader() != null)
                        .map(Class::getName)
                        .collect(Collectors.toList());

        classesSet.addAll(classNames);
        allClasses = new ArrayList<>();
        allClasses.addAll(classesSet);

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                if (loader != null && className != null
                        && !className.startsWith("java/") && !className.startsWith("sun/")
                        && !className.startsWith("jdk/") && !className.startsWith("javax/")
                        && !className.startsWith("com/sun/")) {
                    String name = className.replace('/', '.');
                    if (classNames.add(name)) {
                        allClasses.add(name);
                    }
                }
                return classfileBuffer;
            }
        });
    }

    public static List<String> getAllLoadedClasses() {
        if (allClasses == null) {
            try {
                loadAgent();
            } catch (IOException ex) {
                log.error("Cannot load agent. All {} meta-annotation are ignored. Reason: {}", EncryptedProperty.class.getSimpleName(), ex.toString());
                allClasses.clear();
            }
        }

        return Collections.unmodifiableList(allClasses);
    }

    private static void loadAgent() throws IOException {
        String pid = Arrays.stream(ByteBuddyAgent.ProcessProvider.ForCurrentVm.values())
                .map(ByteBuddyAgent.ProcessProvider.ForCurrentVm::resolve)
                .findFirst().orElseThrow(() -> new EncryptException("Cannot find pid"));

        log.debug("pid: {}", pid);

        File agentJarFile = File.createTempFile("agent-", ".jar");
        try (ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(agentJarFile))) {

            ZipEntry zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
            byte[] data;

            zipFile.putNextEntry(zipEntry);
            data = String.format("Agent-Class: %s\n", EncryptedPropertyAgent.class.getName()).getBytes();
            zipFile.write(data);
            zipFile.closeEntry();

            String agentClassPath = EncryptedPropertyAgent.class.getName().replace('.', '/') + ".class";

            zipEntry = new ZipEntry(agentClassPath);
            zipFile.putNextEntry(zipEntry);
            try (InputStream resourceAsStream = EncryptedPropertyAgent.class.getResourceAsStream("/" + agentClassPath)) {
                int length = resourceAsStream.available();
                data = new byte[length];
                resourceAsStream.read(data);
            }

            zipFile.write(data);
            zipFile.closeEntry();
        }

        try {
            ByteBuddyAgent.attach(agentJarFile, pid);
        } finally {
            agentJarFile.delete();
        }
    }
}
