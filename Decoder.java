import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;



public class Decoder{

    Map<String,String> opCodeMap = new HashMap<>();
    ArrayList<String> instructs = new ArrayList<>();
    ArrayList<String> finalInstructs = new ArrayList<>();
    ArrayList<Integer> labelLines = new ArrayList<>();
    String currInstruct;
    String currOPCode;


    Decoder(String file){
        readFile(file);
        initMaps();
        decodeFile();
    }


    public static void main(String[] args) throws IOException {
        
        // if file not specifed 
        if (args.length < 1){
            System.out.println("Input file not specified. Aborting program");
            System.exit(0);
        } 
        String inputFile = args[0];
        //print the instructions
        Decoder decoder = new Decoder(inputFile);
        decoder.printResult();
    }

    private void readFile(String file){
        Path path = Paths.get(file);
        byte[] fileContents;
        try {
            fileContents = Files.readAllBytes(path);
            StringBuilder instruct = new StringBuilder();
            for (int i = 0; i < fileContents.length/4; i++){
                instruct.setLength(0);
                for (int j = 0; j < 4; j++) {
                    instruct.append(String.format("%8s", Integer.toBinaryString(fileContents[j+(i*4)] & 0xFF)).replace(' ', '0'));
                }
                instructs.add(i, instruct.toString()); 
            }   
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void decodeFile(){
        for (int i = 0; i < instructs.size(); i++){
            String toprint = instructs.get(i);
            String operation = decodeInstruct(toprint, i)+ "     ";
            toprint = toprint.replaceAll("(.{8})", "$1 ");
            String toAdd = String.format("%-25s     %36s%n", operation, toprint);
            finalInstructs.add(toAdd);
        }
    }

    private String decodeInstruct(String code, int lineNum){
        String codeName = null;
        //for loop for finding the op code
        for (int i = 6; i < 12; i++){
            String checkCode = code.substring(0, i);
            if (opCodeMap.containsKey(checkCode)){
                codeName = opCodeMap.get(checkCode);
                break;
            }
        }
        if (codeName != null){
            char instructType = codeName.charAt(0);
            switch (instructType) {
                case 'I':
                    return decodeIType(codeName.substring(2), code);
                case 'R':
                    return decodeRType(codeName.substring(2), code);
                case 'B':
                    return decodeBType(codeName.substring(2), code, lineNum);
                case 'C':
                    return decodeCType(codeName.substring(2), code, lineNum);
                case 'D':
                    return decodeDType(codeName.substring(2), code);
                default:
                    break;
            }
        }
        return codeName;
    }
    private String decodeIType(String op,String code){
        String alu = code.substring(10, 22);
        String RnVal = code.substring(22, 27);
        String RdVal = code.substring(27, 32);

        int num = Integer.parseInt(alu,2);
        String Rn = parseRegisterNum(Integer.parseInt(RnVal,2));
        String Rd = parseRegisterNum(Integer.parseInt(RdVal,2));

        return String.format("%s %s, %s, #%d",op,Rd,Rn,num);
    }
    private String decodeRType(String op,String code){
        String RmVal = code.substring(11, 16);
        String shamtVal = code.substring(16, 22);
        String RnVal = code.substring(22, 27);
        String RdVal = code.substring(27, 32);

        String Rm = parseRegisterNum(Integer.parseInt(RmVal,2));
        String Rn = parseRegisterNum(Integer.parseInt(RnVal,2));
        String Rd = parseRegisterNum(Integer.parseInt(RdVal,2));

        if (op.equals("BR")){
            return String.format("%s %s",op,Rn);
        } else if (op.equals("PRNT")){
            return String.format("%s %s",op,Rd);
        } else if (op.equals("PRNL")|| op.equals("DUMP") || op.equals("HALT")){
            return String.format("%s",op);
        } else {
            return String.format("%s %s, %s, %s",op,Rd,Rn,Rm);
        }
    }
    private String decodeBType(String op,String code, int lineNum){
        String addrVal = code.substring(6, 32);
        int addr = parseNum(addrVal);
        labelLines.add(lineNum + addr);
        return String.format("%s label%d", op, addr+lineNum);
        
    }
    private String decodeCType(String op,String code, int lineNum){
        String addrVal = code.substring(8, 27);
        String RtVal = code.substring(27, 32);
        int addr = parseNum(addrVal);
        labelLines.add(lineNum + addr);
        if (op.equals("B.")){
            String Rt = parseCond(Integer.parseInt(RtVal,2));
            return String.format("%s%s label%d", op, Rt, addr+lineNum);

        } else {
            String Rt = parseRegisterNum(Integer.parseInt(RtVal,2));
            return String.format("%s %s, label%d", op, Rt, addr+lineNum);
        }
    }

    private String decodeDType(String op,String code){
        String addrVal = code.substring(11, 20);
        String op2Val = code.substring(20, 22);
        String RnVal = code.substring(22, 27);
        String RtVal = code.substring(27, 32);

        int addr = parseNum(addrVal);
        String Rn = parseRegisterNum(Integer.parseInt(RnVal,2));
        String Rt = parseRegisterNum(Integer.parseInt(RtVal,2));
        return String.format("%s %s, [%s, #%s]",op,Rt,Rn,addr);
    }

    private String parseRegisterNum(int register){
        switch (register) {
            case 31:
                return "XZR";
            case 30:
                return "LR";
            case 29:
                return "FP";
            case 28:
                return "SP";
            default:
                String returnVal = "X" + register;
                return returnVal;
        }
    }

    private String parseCond(int cond){
switch (cond) {
    case 0:
        return "EQ";
    case 1:
        return "NE";
    case 2:
        return "HS";
    case 3:
        return "LO";
    case 4:
        return "MI";
    case 5:
        return "PL";
    case 6:
        return "VS";
    case 7:
        return "VC";
    case 8:
        return "HI";
    case 9:
        return "LS";
    case 10:
        return "GE";
    case 11:
        return "LT";
    case 12:
        return "GT";
    case 13:
        return "LE";

    default:
        return "";
}
    }
    private void printResult(){
        for (int i = 0; i < finalInstructs.size(); i++){
            if (labelLines.contains(i)){
                System.out.format("label%d:%n",i);
            }
            System.out.format("%3d: %s",i,finalInstructs.get(i));
        }
    }
    private int parseNum(String value){
        if (value.charAt(0) == '0'){
            return Integer.parseInt(value, 2);
        } else {
            value = value.replaceAll("0", "2");
            value = value.replaceAll("1", "0");
            value = value.replaceAll("2", "1");
            return (Integer.parseInt(value, 2)+1) * -1;
        }
    }

    private void initMaps(){
        opCodeMap.put("10001011000", "R:ADD");
        opCodeMap.put("1001000100", "I:ADDI");
        opCodeMap.put("10001010000", "R:AND");
        opCodeMap.put("1001001000", "I:ANDI");
        opCodeMap.put("000101", "B:B");
        opCodeMap.put("01010100", "C:B.");
        opCodeMap.put("100101", "B:BL");
        opCodeMap.put("11010110000", "R:BR");
        opCodeMap.put("10110100", "C:CBZ");
        opCodeMap.put("10110101", "C:CBNZ");
        opCodeMap.put("11001010000", "R:EOR");
        opCodeMap.put("1101001000", "I:EORI");
        opCodeMap.put("11111000010", "D:LDUR");
        opCodeMap.put("11010011011", "R:LSL");
        opCodeMap.put("11010011010", "R:LSR");
        opCodeMap.put("10101010000", "R:ORR");
        opCodeMap.put("1011001000", "I:ORRI");
        opCodeMap.put("11111000000", "D:SDUR");
        opCodeMap.put("11001011000", "R:SUB");
        opCodeMap.put("1101000100", "I:SUBI");
        opCodeMap.put("1111000100", "I:SUBIS");
        opCodeMap.put("11101011000", "R:SUBS");
        opCodeMap.put("10011011000", "R:MUL");
        opCodeMap.put("11111111101", "R:PRNT");
        opCodeMap.put("11111111100", "R:PRNL");
        opCodeMap.put("11111111110", "R:DUMP");
        opCodeMap.put("11111111111", "R:HALT");

        //opCodeMap.put("1011000100", "I:ADDIS"); //optional
        //opCodeMap.put("1111001000", "I:ANDIS"); //optional


    }
}