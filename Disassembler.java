import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Disassembler {
    private static final List<Integer> file = new ArrayList<>();
    public static void main(String[] args) {
        try (InputStream reader = new FileInputStream(args[0])) {
            int read = reader.read();
            while (read != -1) {
                file.add(read);
                read = reader.read();
            }

            ELFParser parser = new ELFParser(file);

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(args[1]), StandardCharsets.UTF_8)))
            {
                writeInFile(parser, writer);
            } catch (IOException e) {
                System.out.println("Output error, i give up! " + e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Input error, i give up! " + e.getMessage());
        }
    }

    private static void writeInFile(ELFParser parser, BufferedWriter writer) throws IOException {
        writer.write("Disassembly of section .text:");
        writer.newLine();
        int addr = parser.TEXT_VIRTUAL_ADDRESS;
        for (int i = 0; i < parser.COMMAND_COUNT; i++) {
            String addressName = parser.getAddressName(addr);
            if (!addressName.isEmpty()) {
                writer.newLine();
                writer.write(String.format("%08x   <%s>:\n", addr, addressName));
            }
            writer.write(parser.getCommandString(i));
            writer.newLine();
            addr += 4;
        }

        writer.newLine();
        writer.write("SYMBOL TABLE:");
        writer.newLine();
        writer.write(parser.getSymbolTableString());
    }
}
