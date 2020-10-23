package io.qbilon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello World!");
        Path path = Paths.get("/workspace", "qif2csv", "weg.qif");
        String content = Files.readString(path, StandardCharsets.ISO_8859_1);

        List<Transaction> ts = new ArrayList<>();
        String[] transactionsS = content.split("\\^");
        for (String transactionS : transactionsS) {
            String[] lines = transactionS.split(System.getProperty("line.separator"));
            Transaction t = new Transaction();
            
            for (String line : lines) {
                if(!line.isBlank()){
                    switch (line.charAt(0)) {
                        case 'D':
                            // Date in the format d(d)/m(m)'yy, e.g., 2/1'19
                            String ds = line.substring(1);
                            String day = ds.substring(0, ds.indexOf("/")).trim();
                            String month = ds.substring(ds.indexOf("/")+1, ds.indexOf("'")).trim();
                            String year = "20" + ds.substring(ds.indexOf("'")+1).trim();
    
                            String dsEnhanced = day + "-" + month + "-" + year;
                            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                            t.date = format.parse(dsEnhanced);  
                            break;
                        case 'T':
                             // Transaction: negative = paying, positive = receiving
                            String value1 = line.substring(1).replace(",", ""); // values like 2,000.00 -> 2000.00
                            Double amount = Double.parseDouble(value1.trim()) ;
                            if (amount < 0) {
                                t.spend = amount;
                            } else {
                                t.receive = amount;
                            }
                            break;
                        case 'N':
                            String value2 = line.substring(1);
                            t.id = Integer.parseInt(value2.trim());
                            break;
                        case 'P':
                            String value3 = line.substring(1);
                            t.payee = value3.trim();
                            break;
                        case 'L':
                            String value4 = line.substring(1).trim();
                            t.label = value4;
                            break;
                        case '^':
                            // ignore
                            break;
                        case 'M':
                            // sometimes seems to be an additional note and sometimes just the month
                            String value5 = line.substring(1).trim();
                            if(value5.matches("[Januar,Februar,März,April,Mai,Juni,Juli,August,September,Oktober,November,Dezember]")){
                                // ignore
                            } else {
                                t.additionalNotes.add(value5);
                            }
                            break;
                        case 'S':
                            String s = line.substring(1).trim();
                            t.additionalNotes.add(s);
                            break;
                        case 'E':
                            String e = line.substring(1).trim();
                            t.additionalNotes.add(e);
                            break;
                        case '$':
                            String s$ = line.substring(1).trim();
                            t.additionalNotes.add(s$);
                            break;
                        default:
                            System.out.println("Invalid token for line: " + line);
                            break;
                    }
                }
            }
            ts.add(t);
        }

        System.out.println("Nr of entries " + ts.size());

        Path toPath = Paths.get("/workspace/qif2csv/weg.csv");

        StringBuilder sb = new StringBuilder();
        sb.append("id # date # payee # label # additionalNotes # spend # receive");
        sb.append("\n");

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        DecimalFormat numberFormat = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.GERMAN));
        for (Transaction t : ts) {
            sb.append(t.id + " # ");
            if(t.date != null)
                sb.append(dateFormat.format(t.date) + " # ");
            sb.append(t.payee + " # " );
            sb.append(t.label + " # ");
            sb.append(String.join(" || ", t.additionalNotes) + " # ");
            sb.append(numberFormat.format(t.spend) + " € # ");
            sb.append(numberFormat.format(t.receive) + " € # ");
            sb.append("\n");
        }


        byte[] strToBytes = sb.toString().getBytes();
        Files.write(toPath, strToBytes);

        System.out.println("Finished");

    }

    private static class Transaction {
        public int id = -1;
        public Date date;
        public String payee = "";
        public String label = "";
        public List<String> additionalNotes = new ArrayList<>();
        public double spend;
        public double receive;
    }
}
