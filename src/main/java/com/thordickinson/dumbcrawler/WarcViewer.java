package com.thordickinson.dumbcrawler;

import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.awt.Desktop;

public class WarcViewer {

    public static void main(String[] args) {
        var path = Path.of(System.getProperty("user.home"), ".crawler").resolve("jobs");
        Scanner scanner = new Scanner(System.in);
        try {
            while (true) {
                // Get the list of WARC files in the directory (recursively)
                List<Path> warcFiles = getWarcFiles(path);

                if (warcFiles.isEmpty()) {
                    System.out.println("No WARC files found in the directory: " + path);
                    System.exit(0);
                }

                // Display the WARC files with an index
                System.out.println("\nWARC Files in Directory (Recursive Search):");
                for (int i = 0; i < warcFiles.size(); i++) {
                    System.out.println(i + ": " + warcFiles.get(i).toString());  // Use full path to identify the files
                }

                // Ask the user to choose a WARC file
                System.out.print("\nEnter the index of the WARC file you want to view (or -1 to exit): ");
                int warcFileIndex = scanner.nextInt();

                if (warcFileIndex == -1) {
                    System.out.println("Exiting...");
                    break;
                }

                if (warcFileIndex < 0 || warcFileIndex >= warcFiles.size()) {
                    System.out.println("Invalid index!");
                    continue;
                }

                // Read the selected WARC file and list its URLs
                Path selectedWarcFile = warcFiles.get(warcFileIndex);
                List<String> urls = listUrlsInWarcFile(selectedWarcFile);

                if (urls.isEmpty()) {
                    System.out.println("No valid URLs found in the WARC file.");
                    continue;
                }

                while (true) {
                    // Display the list of URLs with an index
                    System.out.println("\nURLs in Selected WARC File:");
                    for (int i = 0; i < urls.size(); i++) {
                        System.out.println(i + ": " + urls.get(i));
                    }

                    // Ask the user to select a URL to save
                    System.out.print("\nEnter the index of the URL to save (or -1 to go back to file list): ");
                    int urlIndex = scanner.nextInt();

                    if (urlIndex == -1) {
                        break;  // Go back to file selection
                    }

                    if (urlIndex < 0 || urlIndex >= urls.size()) {
                        System.out.println("Invalid index!");
                        continue;
                    }

                    // Save the selected content to an HTML file
                    saveContentToHtmlFile(selectedWarcFile, urlIndex);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading WARC files.");
            e.printStackTrace();
        }
    }

    /**
     * Recursively scans the directory for WARC files.
     *
     * @param directory The path to the directory containing WARC files.
     * @return A list of paths to WARC files.
     * @throws IOException If there is an error accessing the directory.
     */
    private static List<Path> getWarcFiles(Path directory) throws IOException {
        // Recursively walk the directory and find all .warc files
        try {
            return Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".warc"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error accessing the directory.");
            throw e;
        }
    }

    /**
     * Lists all URLs (WARC-Target-URI) in the given WARC file.
     *
     * @param warcFilePath The path to the WARC file.
     * @return A list of URLs found in the WARC file.
     */
    private static List<String> listUrlsInWarcFile(Path warcFilePath) {
        List<String> urls = new ArrayList<>();

        try {
            // Open the WARC file for reading
            try (WarcReader reader = new WarcReader(Files.newInputStream(warcFilePath))) {
                Iterator<WarcRecord> iterator = reader.iterator();
                int recordCount = 0;

                while (iterator.hasNext()) {
                    WarcRecord record = iterator.next();
                    var url = record.headers().sole("WARC-Target-URI").orElse("N/A");
                    urls.add(url.length() > 100 ? url.substring(0, 100) + "..." : url);
                    recordCount++;
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading WARC file: " + warcFilePath);
            e.printStackTrace();
        }

        return urls;
    }


    /**
     * Saves the content of the selected URL in the WARC file to an HTML file and opens it in the default browser.
     *
     * @param warcFilePath The path to the WARC file.
     * @param urlIndex     The index of the selected URL.
     */
    private static void saveContentToHtmlFile(Path warcFilePath, int urlIndex) {
        try {
            // Open the WARC file for reading
            try (WarcReader reader = new WarcReader(Files.newInputStream(warcFilePath))) {
                Iterator<WarcRecord> iterator = reader.iterator();
                int currentIndex = 0;

                // Find the record with the selected index
                while (iterator.hasNext()) {
                    WarcRecord record = iterator.next();
                    if (currentIndex == urlIndex && record instanceof WarcResponse) {
                        WarcResponse response = (WarcResponse) record;
                        String content = "";
                        try (var body = response.body()) {
                            try (var is = body.stream()) {
                                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                            }
                        }

                        // Create the data directory if it doesn't exist
                        Path dataDir = Path.of("data");
                        if (!Files.exists(dataDir)) {
                            Files.createDirectory(dataDir);
                        }

                        // Create the HTML file with the WARC file name and index
                        String fileName = warcFilePath.getFileName().toString().replace(".warc", "") + "_" + urlIndex + ".html";
                        Path htmlFilePath = dataDir.resolve(fileName);

                        // Save the content to the file
                        Files.write(htmlFilePath, content.getBytes(StandardCharsets.UTF_8));

                        System.out.println("Saved content to: " + htmlFilePath.toAbsolutePath());

                        // Open the saved HTML file in the default system browser
                        openFileInBrowser(htmlFilePath);

                        return;
                    }
                    currentIndex++;
                }
            }

        } catch (IOException e) {
            System.err.println("Error saving content to HTML file.");
            e.printStackTrace();
        }
    }

    /**
     * Opens the given file in the default system browser.
     *
     * @param filePath The path to the file to open.
     */
    private static void openFileInBrowser(Path filePath) {
        try {
            // Check if Desktop API is supported
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    // Open the file in the default system browser
                    desktop.browse(filePath.toUri());
                } else {
                    System.err.println("Opening a browser is not supported on this system.");
                }
            } else {
                System.err.println("Desktop API is not supported on this system.");
            }
        } catch (IOException e) {
            System.err.println("Failed to open the file in the browser.");
            e.printStackTrace();
        }
    }
}


