import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final SymtabSegment[] symbolTable;
    private final Map<Integer, String> labelAddress;
    private int lastLabel = 0;
    public SymbolTable(SymtabSegment[] symbolTable) {
        this.symbolTable = symbolTable;

        labelAddress = new HashMap<>();
        for (SymtabSegment symtabSegment : symbolTable) {
            if (symtabSegment.getType() == 2) {
                labelAddress.put(symtabSegment.getValue(), symtabSegment.getStringName());
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Symbol Value              Size Type 	Bind 	 Vis   	   Index Name\n");
        for (int i = 0; i < symbolTable.length; i++) {
            result.append(String.format("[%4d] 0x%-15X %5d %-8s %-8s %-8s %6s %s\n",
                    i,
                    symbolTable[i].getValue(),
                    symbolTable[i].getSize(),
                    symbolTable[i].getStringType(),
                    symbolTable[i].getStringBind(),
                    symbolTable[i].getStringVisibility(),
                    symbolTable[i].getStringShndx(),
                    symbolTable[i].getStringName()
            ));
        }
        return result.toString();
    }

    public String getAddressLabel(int address) {
        if (!labelAddress.containsKey(address)) {
            labelAddress.put(address, "L" + lastLabel++);
        }
        return labelAddress.get(address);
    }

    public String getAddressName(int address) {
        return labelAddress.getOrDefault(address, "");
    }
}
