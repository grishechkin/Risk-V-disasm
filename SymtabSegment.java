import java.util.List;

public class SymtabSegment {
    private final String stringName;
    private final int name;
    private final int value;
    private final int size;
    private final int info;
    private final int other;
    private final int shndx;
    private final int type;
    private final int bind;
    private final int visibility;

    public SymtabSegment(int name, int value, int size, int info, int other, int shndx, List<Integer> stringTable) {
        this.name = name;
        this.value = value;
        this.size = size;
        this.info = info;
        this.other = other;
        this.shndx = shndx;
        this.stringName = findStringName(stringTable);

        this.bind = this.info >> 4;
        this.type = this.info & 0xf;
        this.visibility = this.other & 0x3;
    }

    public int getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public int getSize() {
        return size;
    }

    public int getInfo() {
        return info;
    }

    public int getOther() {
        return other;
    }

    public int getShndx() {
        return shndx;
    }

    public int getType() {
        return type;
    }

    public int getBind() {
        return bind;
    }

    public int getVisibility() {
        return visibility;
    }

    public String findStringName(List<Integer> stringTable) {
        int pos = name;
        StringBuilder result = new StringBuilder();
        while (stringTable.get(pos) != 0) {
            result.append((char)(int)stringTable.get(pos++));
        }
        return result.toString();
    }

    public String getStringType() {
        return switch(type) {
            case 0 -> "NOTYPE";
            case 1 -> "OBJECT";
            case 2 -> "FUNC";
            case 3 -> "SECTION";
            case 4 -> "FILE";
            case 5 -> "COMMON";
            case 6 -> "TLS";
            case 10 -> "LOOS";
            case 12 -> "HIOS";
            case 13 -> "LOPROC";
            case 15 -> "HIPROC";
            default -> {
                throw new UnsupportedOperationException("Unsupported symtab segment type");
            }
        };
    }

    public String getStringBind() {
        return switch(bind) {
            case 0 -> "LOCAL";
            case 1 -> "GLOBAL";
            case 2 -> "WEAK";
            case 10 -> "LOOS";
            case 12 -> "HIOS";
            case 13 -> "LOPROC";
            case 15 -> "HIPROC";
            default -> {
                throw new UnsupportedOperationException("Unsupported symtab segment bind");
            }
        };
    }

    public String getStringVisibility() {
        return switch(visibility) {
            case 0 -> "DEFAULT";
            case 1 -> "INTERNAL";
            case 2 -> "HIDDEN";
            case 3 -> "PROTECTED";
            case 4 -> "EXPORTED";
            case 5 -> "SINGLETON";
            case 6 -> "ELIMINATE";
            default -> {
                throw new UnsupportedOperationException("Unsupported symtab segment visibility");
            }
        };
    }

    public String getStringShndx() {
        return switch(shndx) {
            case 0 -> "UNDEF";
            case 0xff00 -> "LORESERVE";
            case 0xff01 -> "AFTER";
            case 0xff02 -> "AMD64_LCOMMON";
            case 0xff1f -> "HIPROC";
            case 0xff20 -> "LOOS";
            case 0xff3f -> "HIOS";
            case 0xfff1 -> "ABS";
            case 0xfff2 -> "COMMON";
            case 0xffff -> "XINDEX";
            default -> Integer.toString(shndx);
        };
    }

    public String getStringName() {
        return stringName;
    }
}
