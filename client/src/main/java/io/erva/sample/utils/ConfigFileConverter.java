package io.erva.sample.utils;

import java.io.BufferedReader;
import java.io.IOException;

public class ConfigFileConverter {
    public static String readAndFixConfigFile(BufferedReader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        String line;
        StringBuilder tempLines = new StringBuilder();
        boolean isConnectionBlock = false;

        while ((line = reader.readLine()) != null) {
            if(line.contains("<connection>")){
                isConnectionBlock = true;
            }
            if(isConnectionBlock){
                tempLines.append(line);
                tempLines.append('\n');
                if(line.contains("</connection>")){
                    isConnectionBlock = false;

                    if(!tempLines.toString().matches("(?s)(.*)remote(?s)(.*)tcp(?s)(.*)")){
                        result.append(tempLines);
                    }
                    tempLines = new StringBuilder();
                }
                continue;
            }
            result.append(line);
            result.append('\n');
        }
        return result.toString();
    }
}
