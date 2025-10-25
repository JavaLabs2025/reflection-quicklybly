package org.example.generator;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PackageUtils {

    private PackageUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Set<Class<?>> getClassesInPackage(String packageName, ClassLoader classLoader)
            throws IOException, URISyntaxException {
        Set<Class<?>> classes = new HashSet<>();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                File directory = new File(resource.toURI());
                scanDirectory(directory, packageName, classes, classLoader);
            } else if ("jar".equals(resource.getProtocol())) {
                JarFile jar = ((JarURLConnection) resource.openConnection()).getJarFile();
                scanJar(jar, packageName, classes, classLoader);
            }
        }

        return classes;
    }

    private static void scanDirectory(File dir, String packageName, Set<Class<?>> classes, ClassLoader classLoader) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                scanDirectory(file, subPackage, classes, classLoader);
            } else if (file.getName().endsWith(".class")) {
                String className = file.getName().substring(0, file.getName().length() - 6);
                if (!packageName.isEmpty()) {
                    className = packageName + "." + className;
                }
                try {
                    classes.add(classLoader.loadClass(className));
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
        }
    }

    private static void scanJar(JarFile jar, String packageName, Set<Class<?>> classes, ClassLoader classLoader) {
        String packagePath = packageName.replace('.', '/');
        Enumeration<JarEntry> entries = jar.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.startsWith(packagePath) && name.endsWith(".class") && !entry.isDirectory()) {
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                try {
                    classes.add(classLoader.loadClass(className));
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
        }
    }
}
