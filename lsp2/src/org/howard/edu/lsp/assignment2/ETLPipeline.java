package org.howard.edu.lsp.assignment2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;


public class ETLPipeline {

    private static final String INPUT_PATH = "lsp2/data/employees.csv";
    private static final String OUTPUT_PATH = "lsp2/data/transformed_employees.csv";

    private static class TransformedRecord {
        int employeeId;
        String name;
        String department;
        BigDecimal hoursWorked;
        BigDecimal hourlyRate;
        BigDecimal grossPay;
        String payLevel;
        String employmentStatus;
    }

    public static void main(String[] args) {
        int rowsRead = 0;
        int rowsTransformed = 0;
        int rowsSkipped = 0;

        List<TransformedRecord> output = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_PATH))) {
            String line = reader.readLine();

            if (line == null) {
                printSummary(rowsRead, rowsTransformed, rowsSkipped);
                return;
            }

            while ((line = reader.readLine()) != null) {
                rowsRead++;

                TransformedRecord record = processRow(line);
                if (record == null) {
                    rowsSkipped++;
                } else {
                    output.add(record);
                    rowsTransformed++;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading input file: " + e.getMessage());
            return;
        }

        writeOutput(output);
        printSummary(rowsRead, rowsTransformed, rowsSkipped);
    }

    private static TransformedRecord processRow(String line) {
        if (line.trim().isEmpty()) {
            return null;
        }

        String[] rawFields = line.split(",", -1);
        if (rawFields.length != 5) {
            return null;
        }

        String employeeIdStr = rawFields[0].trim();
        String nameStr = rawFields[1].trim().toUpperCase();
        String departmentStr = rawFields[2].trim();
        String hoursWorkedStr = rawFields[3].trim();
        String hourlyRateStr = rawFields[4].trim();

        int employeeId;
        try {
            employeeId = Integer.parseInt(employeeIdStr);
        } catch (NumberFormatException e) {
            return null;
        }

        BigDecimal hoursWorked;
        try {
            hoursWorked = new BigDecimal(hoursWorkedStr);
        } catch (NumberFormatException e) {
            return null;
        }

        BigDecimal hourlyRate;
        try {
            hourlyRate = new BigDecimal(hourlyRateStr);
        } catch (NumberFormatException e) {
            return null;
        }

        if (hoursWorked.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }
        if (hourlyRate.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }

        BigDecimal FORTY = new BigDecimal("40.00");
        BigDecimal grossPay;
        if (hoursWorked.compareTo(FORTY) <= 0) {
            grossPay = hoursWorked.multiply(hourlyRate);
        } else {
            BigDecimal overtimeHours = hoursWorked.subtract(FORTY);
            BigDecimal regularPay = FORTY.multiply(hourlyRate);
            BigDecimal overtimePay = overtimeHours.multiply(hourlyRate).multiply(new BigDecimal("1.5"));
            grossPay = regularPay.add(overtimePay);
        }

        if (departmentStr.equals("IT")) {
            grossPay = grossPay.multiply(new BigDecimal("1.05"));
        }

        grossPay = grossPay.setScale(2, RoundingMode.HALF_UP);

        String payLevel = determinePayLevel(grossPay);

        String employmentStatus = hoursWorked.compareTo(new BigDecimal("30.00")) >= 0
                ? "Full-Time"
                : "Part-Time";

        TransformedRecord record = new TransformedRecord();
        record.employeeId = employeeId;
        record.name = nameStr;
        record.department = departmentStr;
        record.hoursWorked = hoursWorked;
        record.hourlyRate = hourlyRate;
        record.grossPay = grossPay;
        record.payLevel = payLevel;
        record.employmentStatus = employmentStatus;

        return record;
    }

    private static String determinePayLevel(BigDecimal grossPay) {
        if (grossPay.compareTo(new BigDecimal("500.00")) < 0) {
            return "Low";
        } else if (grossPay.compareTo(new BigDecimal("1000.00")) < 0) {
            return "Standard";
        } else if (grossPay.compareTo(new BigDecimal("2000.00")) < 0) {
            return "High";
        } else {
            return "Executive";
        }
    }

    private static void writeOutput(List<TransformedRecord> records) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_PATH))) {
            writer.println("EmployeeID,Name,Department,HoursWorked,HourlyRate,GrossPay,PayLevel,EmploymentStatus");

            for (TransformedRecord r : records) {
                writer.println(
                        r.employeeId + "," +
                        r.name + "," +
                        r.department + "," +
                        r.hoursWorked.setScale(2, RoundingMode.HALF_UP) + "," +
                        r.hourlyRate.setScale(2, RoundingMode.HALF_UP) + "," +
                        r.grossPay.setScale(2, RoundingMode.HALF_UP) + "," +
                        r.payLevel + "," +
                        r.employmentStatus
                );
            }
        } catch (IOException e) {
            System.out.println("Error writing output file: " + e.getMessage());
        }
    }

    private static void printSummary(int rowsRead, int rowsTransformed, int rowsSkipped) {
        System.out.println("Rows read: " + rowsRead);
        System.out.println("Rows transformed: " + rowsTransformed);
        System.out.println("Rows skipped: " + rowsSkipped);
        System.out.println("Output file: " + OUTPUT_PATH);
    }
}