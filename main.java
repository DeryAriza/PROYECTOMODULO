import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Segunda clase ejecutable del proyecto "Generación y clasificación de datos".
 *
 * <p>Lee los archivos planos generados por {@link GenerateInfoFiles} en la
 * carpeta {@code ARCHIVOS/entrada} y crea dos reportes CSV en la carpeta
 * {@code ARCHIVOS/salida}:</p>
 * <ol>
 *   <li>{@code reporte_vendedores.csv}: nombre de cada vendedor y el dinero
 *       que recaudó, ordenado de mayor a menor.</li>
 *   <li>{@code reporte_productos.csv}: nombre y precio de los productos
 *       vendidos, ordenados por cantidad vendida de mayor a menor.</li>
 * </ol>
 *
 * <p>El programa no solicita información al usuario. Al terminar muestra un
 * mensaje de finalización exitosa o un mensaje de error.</p>
 *
 * <p>Nota: el nombre de la clase ({@code main}, en minúscula) corresponde a
 * lo exigido en el enunciado del proyecto.</p>
 *
 * @author Deri Yamile Chaparro Ariza
 * @author Pablo Andrés Castellanos Aza
 * @author Hollys Casas Sarmiento
 * @version 2.0
 */
public class main {

    /** Carpeta donde se encuentran los archivos de entrada. */
    private static final Path INPUT_FOLDER = Paths.get("ARCHIVOS", "entrada");

    /** Carpeta donde se escriben los reportes generados. */
    private static final Path OUTPUT_FOLDER = Paths.get("ARCHIVOS", "salida");

    /** Nombre del archivo con la información de los vendedores. */
    private static final String SALESMEN_FILE = "vendedores.txt";

    /** Nombre del archivo con la información de los productos. */
    private static final String PRODUCTS_FILE = "productos.txt";

    /** Nombre del reporte de vendedores ordenado por dinero recaudado. */
    private static final String SALESMEN_REPORT = "reporte_vendedores.csv";

    /** Nombre del reporte de productos ordenado por cantidad vendida. */
    private static final String PRODUCTS_REPORT = "reporte_productos.csv";

    /** Separador de campos usado en todos los archivos. */
    private static final String SEPARATOR = ";";

    /**
     * Punto de entrada del programa. Carga productos, vendedores y ventas,
     * calcula los totales y genera los dos reportes CSV.
     *
     * @param args no se utilizan
     */
    public static void main(String[] args) {

        try {

            Map<String, Product> products = loadProducts();

            Map<String, Salesman> salesmen = loadSalesmen();

            processSalesFiles(products, salesmen);

            Files.createDirectories(OUTPUT_FOLDER);

            createSalesmenReport(salesmen);

            createProductsReport(products);

            System.out.println(
                "Reportes generados correctamente en la carpeta "
                + OUTPUT_FOLDER
            );

        } catch (IOException e) {

            System.err.println(
                "Ocurrio un error al generar los reportes. Verifique que "
                + "exista la carpeta " + INPUT_FOLDER
                + " (ejecute primero GenerateInfoFiles)."
            );
            System.err.println("Detalle: " + e.getMessage());

        } catch (RuntimeException e) {

            System.err.println("Ocurrio un error inesperado:");
            e.printStackTrace();
        }
    }

    /**
     * Lee el archivo productos.txt.
     *
     * <p>Formato de cada línea: {@code IDProducto;NombreProducto;PrecioPorUnidad}</p>
     *
     * @return mapa de productos indexado por su identificador, en el mismo
     *         orden del archivo
     * @throws IOException si el archivo no existe o no se puede leer
     */
    private static Map<String, Product> loadProducts() throws IOException {

        Map<String, Product> products = new LinkedHashMap<>();

        for (String[] fields : readLines(INPUT_FOLDER.resolve(PRODUCTS_FILE))) {

            if (fields.length < 3) {
                System.out.println(
                    "Advertencia: linea de producto incompleta ignorada."
                );
                continue;
            }

            String productId = fields[0];
            String productName = fields[1];
            long price = parseNumber(fields[2]);

            if (price < 0) {
                System.out.println(
                    "Advertencia: producto " + productId
                    + " con precio invalido, se ignora."
                );
                continue;
            }

            products.put(productId, new Product(productName, price));
        }

        return products;
    }

    /**
     * Lee el archivo vendedores.txt.
     *
     * <p>Formato de cada línea:
     * {@code TipoDocumento;NumeroDocumento;Nombres;Apellidos}</p>
     *
     * @return mapa de vendedores indexado por la llave
     *         {@code TipoDocumento;NumeroDocumento}
     * @throws IOException si el archivo no existe o no se puede leer
     */
    private static Map<String, Salesman> loadSalesmen() throws IOException {

        Map<String, Salesman> salesmen = new LinkedHashMap<>();

        for (String[] fields : readLines(INPUT_FOLDER.resolve(SALESMEN_FILE))) {

            if (fields.length < 4) {
                System.out.println(
                    "Advertencia: linea de vendedor incompleta ignorada."
                );
                continue;
            }

            String documentKey = fields[0] + SEPARATOR + fields[1];
            String fullName = fields[2] + " " + fields[3];

            salesmen.put(documentKey, new Salesman(fullName));
        }

        return salesmen;
    }

    /**
     * Recorre todos los archivos de ventas de la carpeta de entrada y
     * acumula el dinero recaudado por vendedor y la cantidad vendida por
     * producto.
     *
     * <p>Se considera archivo de ventas todo archivo {@code .txt} de la
     * carpeta de entrada distinto de vendedores.txt y productos.txt. Si un
     * vendedor tiene varios archivos, sus ventas se suman.</p>
     *
     * @param products  productos cargados previamente
     * @param salesmen  vendedores cargados previamente
     * @throws IOException si algún archivo no se puede leer
     */
    private static void processSalesFiles(
            Map<String, Product> products,
            Map<String, Salesman> salesmen) throws IOException {

        try (DirectoryStream<Path> files =
                 Files.newDirectoryStream(INPUT_FOLDER, "*.txt")) {

            for (Path file : files) {

                String fileName = file.getFileName().toString();

                if (fileName.equals(SALESMEN_FILE)
                        || fileName.equals(PRODUCTS_FILE)) {
                    continue;
                }

                processSalesFile(file, products, salesmen);
            }
        }
    }

    /**
     * Procesa un archivo de ventas de un vendedor.
     *
     * <p>Formato: primera línea {@code TipoDocumento;NumeroDocumento} y
     * luego una venta por línea {@code IDProducto;Cantidad;} (el punto y
     * coma final es opcional).</p>
     *
     * @param file      archivo de ventas a procesar
     * @param products  productos disponibles
     * @param salesmen  vendedores registrados
     * @throws IOException si el archivo no se puede leer
     */
    private static void processSalesFile(
            Path file,
            Map<String, Product> products,
            Map<String, Salesman> salesmen) throws IOException {

        List<String[]> lines = readLines(file);

        String fileName = file.getFileName().toString();

        if (lines.isEmpty() || lines.get(0).length < 2) {
            System.out.println(
                "Advertencia: archivo " + fileName
                + " sin identificacion de vendedor, se ignora."
            );
            return;
        }

        String documentKey = lines.get(0)[0] + SEPARATOR + lines.get(0)[1];
        Salesman salesman = salesmen.get(documentKey);

        if (salesman == null) {
            System.out.println(
                "Advertencia: el vendedor " + documentKey + " del archivo "
                + fileName + " no existe en " + SALESMEN_FILE + ", se ignora."
            );
            return;
        }

        for (int i = 1; i < lines.size(); i++) {

            String[] fields = lines.get(i);

            if (fields.length < 2) {
                continue;
            }

            Product product = products.get(fields[0]);
            long quantity = parseNumber(fields[1]);

            if (product == null || quantity < 0) {
                System.out.println(
                    "Advertencia: venta invalida en " + fileName
                    + " (linea " + (i + 1) + "), se ignora."
                );
                continue;
            }

            salesman.totalSales += product.price * quantity;
            product.quantitySold += quantity;
        }
    }

    /**
     * Crea el reporte de vendedores ordenado por dinero recaudado, de mayor
     * a menor.
     *
     * <p>Formato de cada línea: {@code NombreVendedor;DineroRecaudado}</p>
     *
     * @param salesmen vendedores con sus totales calculados
     * @throws IOException si el archivo no se puede escribir
     */
    private static void createSalesmenReport(Map<String, Salesman> salesmen)
            throws IOException {

        List<Salesman> sortedSalesmen = new ArrayList<>(salesmen.values());

        Collections.sort(
            sortedSalesmen,
            Comparator.comparingLong((Salesman s) -> s.totalSales).reversed()
        );

        Path filePath = OUTPUT_FOLDER.resolve(SALESMEN_REPORT);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {

            for (Salesman salesman : sortedSalesmen) {

                writer.write(
                    salesman.fullName + SEPARATOR + salesman.totalSales
                );

                writer.newLine();
            }
        }

        System.out.println("Reporte de vendedores generado: " + filePath);
    }

    /**
     * Crea el reporte de productos vendidos ordenado por cantidad vendida,
     * de mayor a menor. Solo incluye productos con al menos una unidad
     * vendida.
     *
     * <p>Formato de cada línea: {@code NombreProducto;PrecioPorUnidad}</p>
     *
     * @param products productos con sus cantidades vendidas calculadas
     * @throws IOException si el archivo no se puede escribir
     */
    private static void createProductsReport(Map<String, Product> products)
            throws IOException {

        List<Product> soldProducts = new ArrayList<>();

        for (Product product : products.values()) {
            if (product.quantitySold > 0) {
                soldProducts.add(product);
            }
        }

        Collections.sort(
            soldProducts,
            Comparator.comparingLong((Product p) -> p.quantitySold).reversed()
        );

        Path filePath = OUTPUT_FOLDER.resolve(PRODUCTS_REPORT);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {

            for (Product product : soldProducts) {

                writer.write(product.name + SEPARATOR + product.price);

                writer.newLine();
            }
        }

        System.out.println("Reporte de productos generado: " + filePath);
    }

    /**
     * Lee un archivo de texto y separa cada línea no vacía por punto y coma.
     * Los campos vacíos al final (por ejemplo, por un punto y coma final)
     * se descartan y se eliminan los espacios sobrantes.
     *
     * @param filePath archivo a leer
     * @return lista con los campos de cada línea
     * @throws IOException si el archivo no existe o no se puede leer
     */
    private static List<String[]> readLines(Path filePath) throws IOException {

        List<String[]> lines = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                String[] fields = line.split(SEPARATOR);

                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim();
                }

                lines.add(fields);
            }
        }

        return lines;
    }

    /**
     * Convierte un texto a número entero largo.
     *
     * @param text texto a convertir
     * @return el número, o {@code -1} si el texto no es un número válido
     */
    private static long parseNumber(String text) {

        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Representa un producto y la cantidad total vendida.
     */
    private static class Product {

        /** Nombre del producto. */
        private final String name;

        /** Precio por unidad. */
        private final long price;

        /** Cantidad total vendida entre todos los vendedores. */
        private long quantitySold;

        /**
         * Crea un producto sin ventas.
         *
         * @param name  nombre del producto
         * @param price precio por unidad
         */
        Product(String name, long price) {
            this.name = name;
            this.price = price;
            this.quantitySold = 0;
        }
    }

    /**
     * Representa un vendedor y el dinero total que recaudó.
     */
    private static class Salesman {

        /** Nombres y apellidos del vendedor. */
        private final String fullName;

        /** Dinero total recaudado según los archivos de ventas. */
        private long totalSales;

        /**
         * Crea un vendedor sin ventas.
         *
         * @param fullName nombres y apellidos del vendedor
         */
        Salesman(String fullName) {
            this.fullName = fullName;
            this.totalSales = 0;
        }
    }
}