import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
public class ELFParser {
    private final List<Integer> file;
    private final SymbolTable symbolTable;
    public final int SECTION_HEADER_TABLE_POSITION;
    public static final int SECTION_HEADER_SEGMENT_SIZE = 40;
    public final int STRING_TABLE_HEADER_POSITION;
    public final int STRING_TABLE_POSITION;
    public final int SYMTAB_STRING_TABLE_POSITION;
    public final int SYMTAB_STRING_TABLE_SIZE;
    public final int SECTION_COUNT;
    public final int TEXT_POSITION;
    public final int TEXT_SIZE;
    public final int TEXT_VIRTUAL_ADDRESS;
    public final int SYMBOL_TABLE_POSITION;
    public final int SYMBOL_TABLE_SIZE;
    public final int COMMAND_COUNT;
    public static final int SYMBOL_TABLE_SECTION_SIZE = 16;
    public final int SYMBOL_TABLE_SECTION_COUNT;

    public ELFParser(final List<Integer> file) {
        this.file = new ArrayList<>(file);

        if (file.get(0) != 0x7f || file.get(1) != 0x45 || file.get(2) != 0x4c || file.get(3) != 0x46) {
            throw new UnsupportedOperationException("Unsupported file format");
        }
        if (file.get(4) != 1) {
            throw new UnsupportedOperationException("Supports only 32 bits file");
        }
        if (file.get(5) != 1) {
            throw new UnsupportedOperationException("Supports only little-endian file");
        }

        SECTION_COUNT = getByte(48, 49);
        SECTION_HEADER_TABLE_POSITION = getByte(32, 35);
        STRING_TABLE_HEADER_POSITION = 40 * getByte(50, 51) + SECTION_HEADER_TABLE_POSITION;
        STRING_TABLE_POSITION = getByte(STRING_TABLE_HEADER_POSITION + 0x10, STRING_TABLE_HEADER_POSITION + 0x10 + 4);

        int symbolTablePosition = 0;
        int symbolTableSize = 0;

        int textPosition = 0;
        int textSize = 0;
        int textVirtualAddress = 0;

        int symtabStringTablePosition = 0;
        int symtabStringTableSize = 0;

        for (int i = 0; i < SECTION_COUNT; i++) {
            if (getSectionName(i).equals(".symtab")) {
                symbolTablePosition = getSectionOffset(i);
                symbolTableSize = getSectionSize(i);
            }
            if (getSectionName(i).equals(".text")) {
                textPosition = getSectionOffset(i);
                textSize = getSectionSize(i);
                textVirtualAddress = getSectionVirtualAddress(i);
            }
            if (getSectionName(i).equals(".strtab")) {
                symtabStringTablePosition = getSectionOffset(i);
                symtabStringTableSize = getSectionSize(i);
            }
        }

        if (symbolTablePosition == 0) {
            throw new AssertionError("Symbol table not found");
        }
        SYMBOL_TABLE_POSITION = symbolTablePosition;
        SYMBOL_TABLE_SIZE = symbolTableSize;
        SYMBOL_TABLE_SECTION_COUNT = SYMBOL_TABLE_SIZE / SYMBOL_TABLE_SECTION_SIZE;

        if (textPosition == 0) {
            throw new AssertionError("Text not found");
        }
        TEXT_POSITION = textPosition;
        TEXT_SIZE = textSize;
        COMMAND_COUNT = TEXT_SIZE / 4;
        TEXT_VIRTUAL_ADDRESS = textVirtualAddress;

        if (symtabStringTablePosition == 0) {
            throw new AssertionError("String table for symbol table not found");
        }
        SYMTAB_STRING_TABLE_POSITION = symtabStringTablePosition;
        SYMTAB_STRING_TABLE_SIZE = symtabStringTableSize;
        List<Integer> symtabStringTable = file.subList(SYMTAB_STRING_TABLE_POSITION,
                SYMTAB_STRING_TABLE_POSITION + SYMTAB_STRING_TABLE_SIZE);

        SymtabSegment[] symbolTableSegments = new SymtabSegment[SYMBOL_TABLE_SECTION_COUNT];
        for (int i = 0; i < SYMBOL_TABLE_SECTION_COUNT; i++) {
            symbolTableSegments[i] = new SymtabSegment(
                    getByte(SYMBOL_TABLE_POSITION + i * SYMBOL_TABLE_SECTION_SIZE,
                            SYMBOL_TABLE_POSITION + i * SYMBOL_TABLE_SECTION_SIZE + 3),

                    getByte(SYMBOL_TABLE_POSITION + i * SYMBOL_TABLE_SECTION_SIZE + 4,
                            SYMBOL_TABLE_POSITION + i * SYMBOL_TABLE_SECTION_SIZE + 4 + 3),

                    getByte(SYMBOL_TABLE_POSITION + i * SYMBOL_TABLE_SECTION_SIZE + 8,
                            SYMBOL_TABLE_POSITION + i * SYMBOL_TABLE_SECTION_SIZE + 8 + 3),

                    getByte(SYMBOL_TABLE_POSITION + i * SYMBOL_TABLE_SECTION_SIZE + 12),

                    getByte(SYMBOL_TABLE_POSITION + i * SYMBOL_TABLE_SECTION_SIZE + 13),

                    getByte(SYMBOL_TABLE_POSITION + i * SYMBOL_TABLE_SECTION_SIZE + 14,
                            SYMBOL_TABLE_POSITION + i * SYMBOL_TABLE_SECTION_SIZE + 14 + 1),

                    symtabStringTable
            );
        }
        symbolTable = new SymbolTable(symbolTableSegments);

        for (int i = 0; i < COMMAND_COUNT; i++) {
            getCommandString(i);
        }
    }

    public int getByte(int pos) {
        return file.get(pos);
    }

    public int getByte(int start, int end) {
        int result = 0;
        for (int i = end; i >= start; i--) {
            result = (result << 8) + file.get(i);
        }
        return result;
    }

    public int getSectionHeaderBytes(int section, int start, int end) {
        start += SECTION_HEADER_TABLE_POSITION + section * SECTION_HEADER_SEGMENT_SIZE;
        end += SECTION_HEADER_TABLE_POSITION + section * SECTION_HEADER_SEGMENT_SIZE;
        return getByte(start, end);
    }

    public int getSectionNamePosition(int section) {
        return getSectionHeaderBytes(section, 0, 3);
    }

    public int getSectionOffset(int section) {
        return getSectionHeaderBytes(section, 0x10, 0x10 + 4);
    }

    public int getSectionSize(int section) {
        return getSectionHeaderBytes(section, 0x14, 0x14 + 4);
    }

    public int getSectionVirtualAddress(int section) {
        return getSectionHeaderBytes(section, 0x0c, 0x0c + 3);
    }

    public int getStringTableByte(int pos) {
        return getByte(STRING_TABLE_POSITION + pos);
    }

    public String getSectionName(int sectionPosition) {
        StringBuilder result = new StringBuilder();
        int stringTablePos = getSectionNamePosition(sectionPosition);
        while (getStringTableByte(stringTablePos) != 0) {
            result.append((char) getStringTableByte(stringTablePos++));
        }
        return result.toString();
    }

    public int getCommand(int number) {
        return getByte(number * 4 + TEXT_POSITION, number * 4 + TEXT_POSITION + 3);
    }

    public int getOpcode(int command) {
        return command & 0b1111111;
    }

    public int getFunct3(int command) {
        return (command >> 12) & 0b111;
    }

    public int getFunct7(int command) {
        return command >> 25;
    }

    public String getRegisterName(int reg) {
        return switch (reg) {
            case 0 -> "zero";
            case 1 -> "ra";
            case 2 -> "sp";
            case 3 -> "gp";
            case 4 -> "tp";
            case 5 -> "t0";
            case 6 -> "t1";
            case 7 -> "t2";
            case 8 -> "s0";
            case 9 -> "s1";
            case 10 -> "a0";
            case 11 -> "a1";
            case 12 -> "a2";
            case 13 -> "a3";
            case 14 -> "a4";
            case 15 -> "a5";
            case 16 -> "a6";
            case 17 -> "a7";
            case 18 -> "s2";
            case 19 -> "s3";
            case 20 -> "s4";
            case 21 -> "s5";
            case 22 -> "s6";
            case 23 -> "s7";
            case 24 -> "s8";
            case 25 -> "s9";
            case 26 -> "s10";
            case 27 -> "s11";
            case 28 -> "t3";
            case 29 -> "t4";
            case 30 -> "t5";
            case 31 -> "t6";
            default -> throw new UnsupportedOperationException("Unsupported register: " + "\"" + reg + "\"");
        };
    }

    public int setBits(int number, int bits, int begin, int end) {
        return number % (1 << begin) + ((number >> end) << end) + (bits << begin);
    }

    public int getBits(int number, int begin, int end) {
        int result = 0;
        for (int i = begin; i <= end; i++) {
            if (((number >> i) & 1) == 1) {
                result |= (1 << i);
            }
        }
        return result >> begin;
    }

    public String getSymbolTableString() {
        return symbolTable.toString();
    }

    public String getAddressLabel(int address) {
        return symbolTable.getAddressLabel(address);
    }

    public String getAddressName(int address) {
        return symbolTable.getAddressName(address);
    }

    public String getCommandString(int number) {
        int addr = TEXT_VIRTUAL_ADDRESS + number * 4;
        int command = getCommand(number);
        StringBuilder parameters = new StringBuilder();
        String commandName = "";
        switch (getOpcode(command)) {
            case 0b0110111 -> {
                commandName = "lui";
                parameters.append(getRegisterName(getBits(command, 7, 11)));
                parameters.append(",");
                parameters.append("0x").append(Integer.toHexString(getBits(command, 12, 31)));
            }
            case 0b0010111 -> {
                commandName = "auipc";
                parameters.append(getRegisterName(getBits(command, 7, 11)));
                parameters.append(",");
                parameters.append("0x").append(Integer.toHexString(getBits(command, 12, 31)));
            }
            case 0b1101111 -> {
                commandName = "jal";
                parameters.append(getRegisterName(getBits(command, 7, 11)));
                parameters.append(",");
                int current = setBits(0, getBits(command, 12, 19), 12, 19);
                current = setBits(current, getBits(command, 20, 20), 11, 11);
                current = setBits(current, getBits(command, 21, 30), 1, 10);
                current = setBits(current, getBits(command, 31, 31), 20, 20);
                parameters.append("0x").append(Integer.toHexString(addr + current));
                parameters.append(" <").append(getAddressLabel(addr + current)).append(">");
            }
            case 0b1100111 -> {
                commandName = "jalr";
                parameters.append(getRegisterName(getBits(command, 7, 11)));
                parameters.append(",");
                parameters.append(getBits(command, 20, 31));
                parameters.append("(");
                parameters.append(getRegisterName(getBits(command, 15, 19)));
                parameters.append(")");
            }
            case 0b1100011 -> {
                switch (getFunct3(command)) {
                    case 0b000 -> commandName = "beq";
                    case 0b001 -> commandName = "bne";
                    case 0b100 -> commandName = "blt";
                    case 0b101 -> commandName = "bge";
                    case 0b110 -> commandName = "bltu";
                    case 0b111 -> commandName = "bgeu";
                    default -> commandName = "unknown_instruction";
                }

                parameters.append(getRegisterName(getBits(command, 15, 19)));
                parameters.append(",");
                parameters.append(getRegisterName(getBits(command, 20, 24)));
                parameters.append(",");
                int current = setBits(0, getBits(command, 7, 7), 11, 11);
                current = setBits(current, getBits(command, 8, 11), 1, 4);
                current = setBits(current, getBits(command, 25, 30), 5, 10);
                current = setBits(current, getBits(command, 31, 31), 12, 12);
                parameters.append("0x").append(Integer.toHexString(addr + current));
                parameters.append(" <").append(getAddressLabel(addr + current)).append(">");
            }
            case 0b0000011 -> {
                switch (getFunct3(command)) {
                    case 0b000 -> commandName = "lb";
                    case 0b001 -> commandName = "lh";
                    case 0b010 -> commandName = "lw";
                    case 0b100 -> commandName = "lbu";
                    case 0b101 -> commandName = "lhu";
                    default -> commandName = "unknown_instruction";
                }
                parameters.append(getRegisterName(getBits(command, 7, 11)));
                parameters.append(",");
                parameters.append(getBits(command, 20, 31));
                parameters.append("(");
                parameters.append(getRegisterName(getBits(command, 15, 19)));
                parameters.append(")");
            }
            case 0b0100011 -> {
                switch (getFunct3(command)) {
                    case 0b000 -> commandName = "sb";
                    case 0b001 -> commandName = "sh";
                    case 0b010 -> commandName = "sw";
                    default -> commandName = "unknown_instruction";
                }
                int current = getBits(command, 7, 11);
                current = setBits(current, getBits(command, 25, 31), 5, 11);
                parameters.append(getRegisterName(getBits(command, 20, 24)));
                parameters.append(",");
                parameters.append(current);
                parameters.append("(");
                parameters.append(getRegisterName(getBits(command, 15, 19)));
                parameters.append(")");
            }
            case 0b0010011 -> {
                switch (getFunct3(command)) {
                    case 0b000 -> commandName = "addi";
                    case 0b010 -> commandName = "slti";
                    case 0b011 -> commandName = "sltiu";
                    case 0b100 -> commandName = "xori";
                    case 0b110 -> commandName = "ori";
                    case 0b111 -> commandName = "andi";
                    case 0b001 -> commandName = "slli";
                    case 0b101 -> {
                        switch (getFunct7(command)) {
                            case 0b0000000 -> commandName = "srli";
                            case 0b0100000 -> commandName = "srai";
                            default -> commandName = "unknown_instruction";
                        }
                    }
                    default -> commandName = "unknown_instruction";
                }
                parameters.append(getRegisterName(getBits(command, 7, 11)));
                parameters.append(",");
                parameters.append(getRegisterName(getBits(command, 15, 19)));
                parameters.append(",");
                parameters.append(getBits(command, 20, 31));
            }
            case 0b0110011 -> {
                switch (getFunct7(command)) {
                    case 0b0000000 -> {
                        switch (getFunct3(command)) {
                            case 0b000 -> commandName = "add";
                            case 0b001 -> commandName = "sll";
                            case 0b010 -> commandName = "slt";
                            case 0b011 -> commandName = "sltu";
                            case 0b100 -> commandName = "xor";
                            case 0b101 -> commandName = "srl";
                            case 0b110 -> commandName = "or";
                            case 0b111 -> commandName = "and";
                        }
                    }
                    case 0b0100000 -> {
                        switch (getFunct3(command)) {
                            case 0b000 -> commandName = "sub";
                            case 0b101 -> commandName = "sra";
                        }
                    }
                    case 0b0000001 -> {
                        switch (getFunct3(command)) {
                            case 0b000 -> commandName = "mul";
                            case 0b001 -> commandName = "mulh";
                            case 0b010 -> commandName = "mulhsu";
                            case 0b011 -> commandName = "mulhu";
                            case 0b100 -> commandName = "div";
                            case 0b101 -> commandName = "divu";
                            case 0b110 -> commandName = "rem";
                            case 0b111 -> commandName = "remu";
                        }
                    }
                    default -> commandName = "unknown_instruction";
                }

                parameters.append(getRegisterName(getBits(command, 7, 11)));
                parameters.append(",");
                parameters.append(getRegisterName(getBits(command, 15, 19)));
                parameters.append(",");
                parameters.append(getRegisterName(getBits(command, 20, 24)));
            }
            case 0b1110011 -> {
                switch (command >> 20) {
                    case 0b000000000000 -> commandName = "ecall";
                    case 0b000000000001 -> commandName = "ebreak";
                    default -> commandName = "unknown_instruction";
                }
            }
            case 0b0001111 -> commandName = "fence";
            default -> commandName = "unknown_instruction";
        }

        return String.format("    %05x:   %08x      %5s %s", addr, command, commandName, parameters);
    }
}
