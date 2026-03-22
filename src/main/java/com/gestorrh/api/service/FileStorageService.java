package com.gestorrh.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Servicio encargado de la gestión de almacenamiento de archivos en el sistema de ficheros local del servidor.
 * <p>
 * Proporciona funcionalidades para la subida segura, recuperación y borrado de archivos, 
 * implementando mecanismos de defensa contra ataques de tipo <i>Path Traversal</i> 
 * y restricciones estrictas sobre las extensiones de archivos permitidas.
 * </p>
 * <p>
 * Los archivos se almacenan bajo nombres únicos generados automáticamente para evitar
 * conflictos y garantizar la privacidad.
 * </p>
 */
@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private final List<String> extensionesPermitidas = Arrays.asList("pdf", "jpg", "jpeg", "png");

    /**
     * Inicializa el servicio configurando el directorio raíz donde se almacenarán los archivos.
     * Si el directorio no existe, intenta crearlo de forma recursiva.
     *
     * @param uploadDir Ruta del directorio de subida (inyectada desde la configuración de la aplicación).
     * @throws RuntimeException Si el directorio no puede ser creado por falta de permisos o errores de E/S.
     */
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo crear el directorio donde se almacenarán los archivos subidos.", ex);
        }
    }

    /**
     * Almacena de forma física un archivo recibido mediante una petición HTTP Multipart.
     * <p>
     * El proceso de guardado incluye:
     * </p>
     * <ol>
     *   <li>Limpieza y normalización de la ruta del archivo original.</li>
     *   <li>Validación contra ataques de secuencia de punto-punto (Path Traversal).</li>
     *   <li>Verificación de la extensión del archivo contra una lista blanca permitida.</li>
     *   <li>Generación de un identificador único universal (UUID) para el nombre del archivo.</li>
     *   <li>Copia binaria del flujo de entrada en la ubicación de almacenamiento configurada.</li>
     * </ol>
     *
     * @param file El objeto {@link MultipartFile} que contiene el flujo binario y metadatos del archivo original.
     * @return String El nombre único generado (UUID + extensión) bajo el cual el archivo ha sido persistido.
     * @throws RuntimeException Si el archivo es nulo, tiene una extensión no autorizada o si ocurre un error de E/S.
     */
    public String guardarArchivo(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if (originalFileName.contains("..")) {
                throw new RuntimeException("El nombre del archivo contiene una secuencia de ruta inválida: " + originalFileName);
            }

            String extension = obtenerExtension(originalFileName).toLowerCase();
            if (!extensionesPermitidas.contains(extension)) {
                throw new RuntimeException("Formato no permitido. Solo se aceptan archivos .pdf, .jpg, .jpeg o .png");
            }

            String fileName = UUID.randomUUID().toString() + "." + extension;

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo guardar el archivo " + originalFileName + ". Por favor, inténtelo de nuevo.", ex);
        }
    }

    /**
     * Recupera un archivo del almacenamiento local como un recurso cargable.
     * Se utiliza para servir archivos (ej. justificantes o imágenes) a través de la API.
     *
     * @param fileName Nombre del archivo que se desea recuperar.
     * @return Resource Objeto cargable que representa el contenido del archivo.
     * @throws RuntimeException Si el archivo no existe o la ruta está mal formada.
     */
    public Resource cargarArchivoComoRecurso(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("Archivo no encontrado: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Archivo no encontrado: " + fileName, ex);
        }
    }

    /**
     * Elimina de forma definitiva un archivo del sistema de ficheros.
     * Se utiliza habitualmente al actualizar archivos antiguos o al borrar registros asociados.
     *
     * @param fileName Nombre del archivo físico que se pretende eliminar.
     */
    public void eliminarArchivo(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            try {
                Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException ex) {
                System.err.println("No se pudo eliminar el archivo antiguo: " + fileName);
            }
        }
    }

    /**
     * Utilidad privada para extraer la extensión de un nombre de archivo completo.
     *
     * @param fileName Nombre original del archivo (ej. "justificante.pdf").
     * @return String La extensión del archivo (ej. "pdf"), o una cadena vacía si no tiene.
     */
    private String obtenerExtension(String fileName) {
        if (fileName != null && fileName.lastIndexOf(".") > 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "";
    }
}
