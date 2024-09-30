package com.thordickinson.dumbcrawler.expression;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Scanner;

public class ExpressionTesterCli {

    private final Scanner scanner = new Scanner(System.in);
    private final URLExpressionEvaluator evaluator = new URLExpressionEvaluator();
    private String url = "https://www.address.com/search/path.php?param1=test,param2=something";

    private String readInput(String prompt) {
        System.out.print("\n" + prompt + "\n>");
        String line = scanner.nextLine();
        return line;
    }

    public String printMenu() {
        boolean invalid = true;
        String input = null;
        do {
            System.out.println("Current url [ %s ]".formatted(url));
            System.out.println("1. Change url");
            System.out.println("2. Evaluate expression");
            System.out.println("3. Print url parts");
            System.out.println("Q. Exit");
            input = readInput("Select an option").toLowerCase();
            if (isAny(input, "1", "2", "3", "q")) {
                invalid = false;
            } else {
                System.out.println("== Invalid input, please select a value ==");
            }
        } while (invalid);
        return input;
    }

    private boolean isAny(String value, String... options) {
        for (String val : options) {
            if (val.equals(value)) return true;
        }
        return false;
    }

    private void updateUri() {
        String uri = readInput("Enter the new url");
        if (StringUtils.isBlank(uri)) {
            System.out.println("Canceled");
            return;
        }
        this.url = uri;
        System.out.println("Url updated!");
    }

    private void printUrlParts() {
        try {
            var parsed = URI.create(url);
            System.out.println("Protocol: " + parsed.getScheme());
            System.out.println("Host: " + parsed.getHost());
            System.out.println("Port: " + parsed.getPort());
            System.out.println("Path: " + parsed.getPath());
            System.out.println("Query: " + parsed.getQuery());
            System.out.println("Fragment: " + parsed.getFragment());
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private void evaluate() {
        String expression = readInput("Enter the expression to evaluate");
        if (StringUtils.isBlank(expression)) {
            System.out.println("Canceled");
            return;
        }
        try {
            var result = evaluator.evaluateBoolean(expression, url);
            System.out.println("Result: " + result);
        } catch (Exception ex) {
            System.out.println("Error evaluating expression: " + ex.toString());
        }
    }

    public void run() {
        String input = null;
        do {
            input = printMenu();
            switch (input) {
                case "1" -> updateUri();
                case "2" -> evaluate();
                case "3" -> printUrlParts();
            }
            readInput("Press any key to continue");
        } while (!"q".equals(input));
        System.out.println("Bye!");
    }

}
