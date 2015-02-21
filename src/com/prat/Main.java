package com.prat;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class Main {

    public static final String SEPARATOR = "\t";
    public static final String NOT_AVAILABLE = "N/A";

    static class Stock {
        final String name;
        final Map<Date, String> data;

        Stock(String name, Map<Date, String> data) {
            this.name = name;
            this.data = data;
        }
    }

    private static final int COL_A = 0;
    private static final int COL_E = 4;

    private static final Set<Date> dates = new TreeSet<>();
    private static final List<Stock> stocks = new ArrayList<>();
    private static final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy");

    private static Map<Date, String> processFile(File file) {
        System.out.println("processing file : " + file.getName());
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            if ((line = reader.readLine()) != null) {
                if (!line.toLowerCase().contains("timestamp") ||
                        !line.toLowerCase().contains("close")) {
                    System.out.println(file.getName() + " doesn't have a correct header!");
                    return null;
                }
            }
            Map<Date, String> map = new TreeMap<>();
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                String[] columns = line.split("\\s+");
                if (columns.length >= 5) {
                    // process the line.

                    try {
                        Date date = format.parse(columns[COL_A]);
                        if (!dates.contains(date)) {
                            dates.add(date);
                        }
                        map.put(date, columns[COL_E]);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            return map;
        } catch (IOException e) {
            System.out.println("Cannot process " + file.getName() + "!");
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    System.out.println("Cannot close " + file.getName() + "!");
                    e.printStackTrace();
                }
        }
        return null;
    }

    private static boolean isExcelFile(Path filePath) {
        return Files.isRegularFile(filePath)
                && filePath.toAbsolutePath().normalize().toString().endsWith(".xls");
    }

    public static boolean isNotOutputFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return !(name.contains("summary") && name.contains(".xls"));
    }

    public static void main(String[] args) {
        System.out.println("Directory : " + Paths.get(".").toAbsolutePath().normalize().toString());
        try {
            Files.walk(Paths.get("."))
                    .filter(Main::isNotOutputFile)
                    .filter(Main::isExcelFile)
                    .forEach(filePath -> {
                        Map<Date, String> m = processFile(filePath.toFile());
                        if (m != null && !m.isEmpty()) {
                            String name = filePath.getFileName().toString();
                            stocks.add(new Stock(name.substring(0, name.indexOf(".xls")), m));
                        }
                    });

            writeOutput();

            System.out.println("Total Excel files: " +
                    Files.walk(Paths.get("."))
                            .filter(Main::isNotOutputFile)
                            .filter(Main::isExcelFile).count());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeOutput() throws FileNotFoundException {
        SimpleDateFormat f = new SimpleDateFormat("_dd-MM_HHmmss");
        String output = "summary" + f.format(System.currentTimeMillis()) + ".xls";
        PrintWriter writer = new PrintWriter(output);

        System.out.println("writing to file : " + output);

        String header = "Timestamp" + SEPARATOR;
        for (Stock stock : stocks) {
            header += stock.name + SEPARATOR;
        }
        writer.println(header.trim());

        dates.forEach(new Consumer<Date>() {
            @Override
            public void accept(Date date) {
                String line = format.format(date) + SEPARATOR;
                for (Stock stock : stocks) {
                    if (stock.data.containsKey(date)) {
                        line += stock.data.get(date) + SEPARATOR;
                    } else {
                        line += NOT_AVAILABLE + SEPARATOR;
                    }
                }
                writer.println(line.trim());
            }
        });

        writer.close();
    }
}
