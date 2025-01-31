package pl.barnatapi.hendiprice;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads";
    private static final String OUTPUT_DIR = "outputs";

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            Files.createDirectories(Paths.get(OUTPUT_DIR));

            Path filePath = Paths.get(UPLOAD_DIR, file.getOriginalFilename());
            Files.write(filePath, file.getBytes());

            String csvFileName = processXmlToCsv(filePath.toString());

            return ResponseEntity.ok("/api/download/" + csvFileName);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Błąd przetwarzania pliku");
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path file = Paths.get(OUTPUT_DIR).resolve(fileName);
            Resource resource = new UrlResource(file.toUri());

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String processXmlToCsv(String inputFilePath) throws IOException {
        String outputFileName = "hendi_prices.csv";
        Path outputFile = Paths.get(OUTPUT_DIR, outputFileName);

        DecimalFormat df = new DecimalFormat("#0.00");
        Pattern skuPattern = Pattern.compile("<property name=\"productCode\">(\\d+)</property>");
        Pattern pricePattern = Pattern.compile("<property name=\"basePrice\">(\\d+\\.?\\d*)</property>");
        Pattern offerPattern = Pattern.compile("<offer>(.*?)</offer>", Pattern.DOTALL);

        String content = new String(Files.readAllBytes(Paths.get(inputFilePath)));

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write("sku,price\n");

            Matcher offerMatcher = offerPattern.matcher(content);
            while (offerMatcher.find()) {
                String offer = offerMatcher.group(1);

                Matcher skuMatcher = skuPattern.matcher(offer);
                Matcher priceMatcher = pricePattern.matcher(offer);

                if (skuMatcher.find() && priceMatcher.find()) {
                    String sku = skuMatcher.group(1);
                    double basePrice = Double.parseDouble(priceMatcher.group(1));
                    double updatedPrice = basePrice * 1.23;
                    writer.write(sku + ",\"" + df.format(updatedPrice) + "\"\n");
                }
            }
        }

        return outputFileName;
    }
}