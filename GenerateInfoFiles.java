import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class GenerateInfoFiles {

    private static final Random RANDOM = new Random();

    public static void main(String[] args) {

        try {

            // Genera el archivo con la información de los productos
            createProductsFile(10);

            // Genera el archivo con la información de los vendedores
            createSalesManInfoFile(10);

            // Genera archivos de ventas para varios vendedores
            createSalesMenFile(5, "vendedor1.txt", 10000001);
            createSalesMenFile(5, "vendedor2.txt", 10000002);
            createSalesMenFile(5, "vendedor3.txt", 10000003);

            System.out.println("Archivos generados correctamente.");

        } catch (IOException e) {

            System.err.println("Ocurrio un error al generar los archivos:");
            e.printStackTrace();
        }
    }

    /**
     * Genera el archivo productos.txt.
     *
     * Formato:
     * IDProducto;NombreProducto;PrecioPorUnidad
     */
    public static void createProductsFile(int productsCount)
            throws IOException {

        Path folderPath = Paths.get("ARCHIVOS", "entrada");

        // Crea la carpeta si no existe
        Files.createDirectories(folderPath);

        Path filePath = folderPath.resolve("productos.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {

            for (int i = 1; i <= productsCount; i++) {

                String productId = String.format("P%03d", i);

                String productName = "Producto" + i;

                int price = (RANDOM.nextInt(10) + 1) * 10000;

                writer.write(
                    productId + ";"
                    + productName + ";"
                    + price
                );

                writer.newLine();
            }
        }

        System.out.println(
            "Archivo de productos generado correctamente."
        );
    }

    /**
     * Genera el archivo vendedores.txt.
     *
     * Formato:
     * TipoDocumento;NumeroDocumento;Nombres;Apellidos
     */
    public static void createSalesManInfoFile(int salesmanCount)
            throws IOException {

        Path folderPath = Paths.get("ARCHIVOS", "entrada");

        // Crea la carpeta si no existe
        Files.createDirectories(folderPath);

        Path filePath = folderPath.resolve("vendedores.txt");

        String[] nombres = {
            "Ana",
            "Carlos",
            "Laura",
            "Andres",
            "Maria",
            "Juan",
            "Sofia",
            "Daniel",
            "Camila",
            "Pedro"
        };

        String[] apellidos = {
            "Gomez",
            "Rodriguez",
            "Martinez",
            "Lopez",
            "Garcia",
            "Perez",
            "Sanchez",
            "Ramirez",
            "Torres",
            "Moreno"
        };

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {

            for (int i = 1; i <= salesmanCount; i++) {

                String tipoDocumento = "CC";

                String numeroDocumento =
                    String.valueOf(10000000 + i);

                String nombre =
                    nombres[(i - 1) % nombres.length];

                String apellido =
                    apellidos[(i - 1) % apellidos.length];

                writer.write(
                    tipoDocumento + ";"
                    + numeroDocumento + ";"
                    + nombre + ";"
                    + apellido
                );

                writer.newLine();
            }
        }

        System.out.println(
            "Archivo de vendedores generado correctamente."
        );
    }

    /**
     * Genera un archivo de ventas para un vendedor.
     *
     * Formato:
     * TipoDocumento;NumeroDocumento
     * IDProducto;Cantidad
     * IDProducto;Cantidad
     * ...
     */
    public static void createSalesMenFile(
            int randomSalesCount,
            String name,
            long id) throws IOException {

        Path folderPath = Paths.get("ARCHIVOS", "entrada");

        // Crea la carpeta si no existe
        Files.createDirectories(folderPath);

        Path filePath = folderPath.resolve(name);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {

            // Primera línea: identificación del vendedor
            writer.write("CC;" + id);
            writer.newLine();

            // Líneas siguientes: productos vendidos
            for (int i = 0; i < randomSalesCount; i++) {

                int productNumber =
                    RANDOM.nextInt(10) + 1;

                String productId =
                    String.format("P%03d", productNumber);

                int quantity =
                    RANDOM.nextInt(10) + 1;

                writer.write(
                    productId + ";" + quantity
                );

                writer.newLine();
            }
        }

        System.out.println(
            "Archivo de ventas generado correctamente: " + name
        );
    }
}