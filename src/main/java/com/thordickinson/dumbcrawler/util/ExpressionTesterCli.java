package com.thordickinson.dumbcrawler.util;

import org.apache.commons.lang3.StringUtils;

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
            System.out.println("Q. Exit");
            input = readInput("Select an option").toLowerCase();
            if (isAny(input, "1", "2", "q")) {
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
        if (StringUtils.isNotBlank(uri)) {
            System.out.println("Canceled");
            return;
        }
        this.url = uri;
        System.out.println("Url updated!");
    }

    private void evaluate() {
        String expression = readInput("Enter the expression to evaluate");
        if (StringUtils.isBlank(expression)) {
            System.out.println("Canceled");
            return;
        }
        try {
            var result = evaluator.evaluate(expression, url);
            System.out.println("Result: " + result);
        } catch (Exception ex) {
            System.out.println("Error evaluating expression: " + ex.toString());
        }
        readInput("Press any key to continue");

    }

    public void run() {
        String input = null;
        do {
            input = printMenu();
            switch (input) {
                case "1" -> updateUri();
                case "2" -> evaluate();
            }
        } while (!"q".equals(input));
        System.out.println("Bye!");
    }

}
