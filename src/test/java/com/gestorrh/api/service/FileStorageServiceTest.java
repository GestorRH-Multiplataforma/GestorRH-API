package com.gestorrh.api.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Nested
    @DisplayName("Tests para guardarArchivo")
    class GuardarArchivoTests {

        @Test
        @DisplayName("Éxito: Guardar archivo PDF correctamente")
        void guardarArchivo_Pdf_Exito() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "contenido pdf".getBytes());

            String fileName = fileStorageService.guardarArchivo(file);

            assertNotNull(fileName);
            assertTrue(fileName.endsWith(".pdf"));
            assertTrue(Files.exists(tempDir.resolve(fileName)));
        }

        @Test
        @DisplayName("Éxito: Guardar archivo imagen correctamente")
        void guardarArchivo_Imagen_Exito() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "foto.png", "image/png", "datos imagen".getBytes());

            String fileName = fileStorageService.guardarArchivo(file);

            assertNotNull(fileName);
            assertTrue(fileName.endsWith(".png"));
            assertTrue(Files.exists(tempDir.resolve(fileName)));
        }

        @Test
        @DisplayName("Falla: Path Traversal detectado")
        void guardarArchivo_PathTraversal_Falla() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "../hack.pdf", "application/pdf", "datos".getBytes());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> 
                    fileStorageService.guardarArchivo(file));
            
            assertTrue(ex.getMessage().contains("secuencia de ruta inválida"));
        }

        @Test
        @DisplayName("Falla: Extensión no permitida")
        void guardarArchivo_ExtensionNoPermitida_Falla() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "virus.exe", "application/octet-stream", "datos".getBytes());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> 
                    fileStorageService.guardarArchivo(file));
            
            assertTrue(ex.getMessage().contains("Formato no permitido"));
        }

        @Test
        @DisplayName("Falla: Archivo sin extensión")
        void guardarArchivo_SinExtension_Falla() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "archivoSinExtension", "text/plain", "datos".getBytes());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> 
                    fileStorageService.guardarArchivo(file));
            
            assertTrue(ex.getMessage().contains("Formato no permitido"));
        }

        @Test
        @DisplayName("Falla: Error de E/S al guardar")
        void guardarArchivo_IoException_Falla() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "datos".getBytes()) {
                @Override
                public java.io.InputStream getInputStream() throws IOException {
                    throw new IOException("Simulated IO error");
                }
            };

            RuntimeException ex = assertThrows(RuntimeException.class, () -> 
                    fileStorageService.guardarArchivo(file));
            
            assertTrue(ex.getMessage().contains("No se pudo guardar el archivo"));
        }
    }

    @Nested
    @DisplayName("Tests para cargarArchivoComoRecurso")
    class CargarArchivoComoRecursoTests {

        @Test
        @DisplayName("Éxito: Cargar archivo existente")
        void cargarArchivo_Existente_Exito() throws IOException {
            String fileName = "test-file.txt";
            Files.write(tempDir.resolve(fileName), "contenido".getBytes());

            Resource resource = fileStorageService.cargarArchivoComoRecurso(fileName);

            assertTrue(resource.exists());
            assertEquals(fileName, resource.getFilename());
        }

        @Test
        @DisplayName("Falla: Cargar archivo inexistente")
        void cargarArchivo_Inexistente_Falla() {
            assertThrows(EntityNotFoundException.class, () -> 
                    fileStorageService.cargarArchivoComoRecurso("no-existe.pdf"));
        }
    }

    @Nested
    @DisplayName("Tests para eliminarArchivo")
    class EliminarArchivoTests {

        @Test
        @DisplayName("Éxito: Eliminar archivo existente")
        void eliminarArchivo_Existente_Exito() throws IOException {
            String fileName = "delete-me.pdf";
            Path filePath = tempDir.resolve(fileName);
            Files.write(filePath, "datos".getBytes());
            assertTrue(Files.exists(filePath));

            fileStorageService.eliminarArchivo(fileName);

            assertFalse(Files.exists(filePath));
        }

        @Test
        @DisplayName("Éxito: No falla si el archivo no existe")
        void eliminarArchivo_Inexistente_NoFalla() {
            assertDoesNotThrow(() -> fileStorageService.eliminarArchivo("no-existe.pdf"));
        }

        @Test
        @DisplayName("Éxito: No falla si el nombre es nulo o vacío")
        void eliminarArchivo_NullVacio_NoFalla() {
            assertDoesNotThrow(() -> fileStorageService.eliminarArchivo(null));
            assertDoesNotThrow(() -> fileStorageService.eliminarArchivo(""));
        }
    }

    @Test
    @DisplayName("Falla: Error al crear el directorio de almacenamiento")
    void constructor_ErrorAlCrearDirectorio_Falla() {
        String invalidPath = tempDir.resolve("archivo_que_impide_directorio").toString();
        try {
            Files.write(tempDir.resolve("archivo_que_impide_directorio"), "datos".getBytes());
        } catch (IOException e) {
            fail("No se pudo preparar el test");
        }

        assertThrows(RuntimeException.class, () -> new FileStorageService(invalidPath));
    }
}
