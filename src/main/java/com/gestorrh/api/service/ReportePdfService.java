package com.gestorrh.api.service;

import com.gestorrh.api.dto.reporte.ReporteDetalleDTO;
import com.gestorrh.api.dto.reporte.ReporteResumenDTO;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio especializado en la generación de documentos PDF para los reportes de asistencia.
 * <p>
 * Utiliza la librería iText (OpenPDF) para la creación programática de documentos complejos,
 * gestionando el diseño corporativo mediante la definición de paletas de colores, fuentes tipográficas
 * y estructuras de tablas personalizadas.
 * </p>
 * <p>
 * Permite exportar tanto la vista detallada de los fichajes diarios como los resúmenes acumulados,
 * proporcionando archivos listos para su impresión o almacenamiento digital.
 * </p>
 */
@Service
@Slf4j
public class ReportePdfService {

    private static final Color COLOR_CORPORATIVO = new Color(41, 128, 185);
    private static final Color GRIS_CLARO = new Color(240, 240, 240);
    private static final Color COLOR_TOTALES = new Color(30, 90, 130);

    private static final Font FUENTE_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_CORPORATIVO);
    private static final Font FUENTE_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
    private static final Font FUENTE_FECHA_GEN = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
    private static final Font FUENTE_CABECERA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FUENTE_CELDA = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

    /**
     * Genera un archivo PDF con el desglose detallado de todos los fichajes realizados por la plantilla.
     * <p>
     * El documento resultante se configura automáticamente en orientación horizontal (Landscape) para 
     * facilitar la lectura de las múltiples columnas de datos (fechas, turnos, entradas, salidas, tiempos y extras).
     * Incluye separadores visuales que agrupan los registros por empleado y departamento.
     * </p>
     *
     * @param nombreEmpresa Nombre oficial de la empresa para ser mostrado en el encabezado principal.
     * @param subtituloFiltro Descripción textual de los filtros aplicados (ej: "Desde 01/01 al 31/01").
     * @param datos Lista de objetos DTO que contienen la información procesada de cada fichaje.
     * @return byte[] El contenido binario del archivo PDF generado, apto para ser enviado en una respuesta HTTP.
     * @throws RuntimeException Si se produce un fallo técnico inesperado durante el flujo de construcción del PDF.
     */
    public byte[] generarPdfDetalle(String nombreEmpresa, String subtituloFiltro, List<ReporteDetalleDTO> datos) {
        log.info("GENERACIÓN PDF: Creando reporte DETALLADO para la empresa '{}'. Procesando {} fichajes.", nombreEmpresa, datos.size());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, out);

            configurarPieDePagina(document);
            document.open();

            agregarCabeceras(document, nombreEmpresa, "Reporte de Asistencia (Detalle) - " + subtituloFiltro);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.5f, 3f, 2.5f, 2.5f, 2.5f, 3f});
            table.setSpacingBefore(15);
            table.setHeaderRows(1);

            agregarCeldaCabecera(table, "Fecha", COLOR_CORPORATIVO);
            agregarCeldaCabecera(table, "Turno", COLOR_CORPORATIVO);
            agregarCeldaCabecera(table, "Entrada", COLOR_CORPORATIVO);
            agregarCeldaCabecera(table, "Salida", COLOR_CORPORATIVO);
            agregarCeldaCabecera(table, "Total (Real)", COLOR_CORPORATIVO);
            agregarCeldaCabecera(table, "Extras", COLOR_CORPORATIVO);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String empleadoActual = "";

            long granTotalMinutos = 0;
            long granTotalExtras = 0;

            for (ReporteDetalleDTO dto : datos) {
                granTotalMinutos += dto.getTiempoTotalMinutos() != null ? dto.getTiempoTotalMinutos() : 0;
                granTotalExtras += dto.getMinutosExtra() != null ? dto.getMinutosExtra() : 0;

                if (!dto.getNombreEmpleado().equals(empleadoActual)) {
                    empleadoActual = dto.getNombreEmpleado();

                    String depto = dto.getDepartamento() != null ? dto.getDepartamento().toUpperCase() : "SIN DEPTO";

                    Font fuenteSeparador = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
                    Phrase tituloSeparador = new Phrase("Fichajes de: " + empleadoActual.toUpperCase() + " [" + depto + "]", fuenteSeparador);

                    PdfPCell separatorCell = new PdfPCell(tituloSeparador);
                    separatorCell.setColspan(6);
                    separatorCell.setBackgroundColor(Color.DARK_GRAY);
                    separatorCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    separatorCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    separatorCell.setPadding(6);
                    table.addCell(separatorCell);
                }

                agregarCeldaDato(table, dto.getFecha() != null ? dto.getFecha().format(formatter) : "-");
                agregarCeldaDato(table, dto.getDescripcionTurno());
                agregarCeldaDato(table, dto.getHoraEntradaReal());
                agregarCeldaDato(table, dto.getHoraSalidaReal());
                agregarCeldaDato(table, formatearMinutos(dto.getTiempoTotalMinutos()));
                agregarCeldaDato(table, formatearMinutos(dto.getMinutosExtra()));
            }

            agregarFilaTotalDetalle(table, granTotalMinutos, granTotalExtras);

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error crítico al generar el PDF Detallado para la empresa '{}': {}", nombreEmpresa, e.getMessage());
            throw new RuntimeException("Error al generar el PDF de Detalle", e);
        }
    }

    /**
     * Genera un archivo PDF con los totales acumulados por cada empleado en el periodo seleccionado.
     * <p>
     * A diferencia del reporte detallado, este documento se presenta en orientación vertical (Portrait)
     * y se centra en métricas agregadas: total de días laborados, suma de horas trabajadas y 
     * acumulado de horas extraordinarias por cada trabajador.
     * </p>
     * <p>
     * Incluye una fila de totales generales al final de la tabla para facilitar la revisión rápida 
     * de costes y tiempos por parte del departamento de administración.
     * </p>
     *
     * @param nombreEmpresa Nombre oficial de la empresa para el encabezado.
     * @param subtituloFiltro Descripción del rango temporal o filtros aplicados (ej: "Resumen Mensual - Marzo").
     * @param datos Lista de objetos {@link ReporteResumenDTO} con los totales consolidados.
     * @return byte[] El contenido binario del archivo PDF generado.
     * @throws RuntimeException Si ocurre un fallo técnico crítico durante el proceso de maquetación del documento.
     */
    public byte[] generarPdfResumen(String nombreEmpresa, String subtituloFiltro, List<ReporteResumenDTO> datos) {
        log.info("GENERACIÓN PDF: Creando reporte RESUMIDO para la empresa '{}'. Procesando totales de {} empleados.", nombreEmpresa, datos.size());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            configurarPieDePagina(document);
            document.open();

            agregarCabeceras(document, nombreEmpresa, "Reporte de Asistencia (Resumen) - " + subtituloFiltro);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f, 1.5f, 2f, 2f});
            table.setSpacingBefore(15);
            table.setHeaderRows(1);

            agregarCeldaCabecera(table, "Empleado", COLOR_CORPORATIVO);
            agregarCeldaCabecera(table, "Departamento", COLOR_CORPORATIVO);
            agregarCeldaCabecera(table, "Días Trab.", COLOR_CORPORATIVO);
            agregarCeldaCabecera(table, "Horas Trab.", COLOR_CORPORATIVO);
            agregarCeldaCabecera(table, "Horas Extra", COLOR_CORPORATIVO);

            long granTotalDias = 0;
            long granTotalMinutos = 0;
            long granTotalExtras = 0;

            for (ReporteResumenDTO dto : datos) {
                granTotalDias += dto.getDiasTrabajados();
                granTotalMinutos += dto.getTotalTiempoTotalMinutos() != null ? dto.getTotalTiempoTotalMinutos() : 0;
                granTotalExtras += dto.getTotalMinutosExtra() != null ? dto.getTotalMinutosExtra() : 0;

                agregarCeldaDato(table, dto.getNombreEmpleado());
                agregarCeldaDato(table, dto.getDepartamento());
                agregarCeldaDato(table, String.valueOf(dto.getDiasTrabajados()));
                agregarCeldaDato(table, formatearMinutos(dto.getTotalTiempoTotalMinutos()));
                agregarCeldaDato(table, formatearMinutos(dto.getTotalMinutosExtra()));
            }

            agregarFilaTotalResumen(table, granTotalDias, granTotalMinutos, granTotalExtras);

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error crítico al generar el PDF Resumido para la empresa '{}': {}", nombreEmpresa, e.getMessage());
            throw new RuntimeException("Error al generar el PDF de Resumen", e);
        }
    }

    /**
     * Construye y añade la sección de cabecera corporativa al documento PDF.
     * <p>
     * Esta sección incluye:
     * </p>
     * <ol>
     *   <li>Nombre de la empresa en mayúsculas y fuente destacada.</li>
     *   <li>Título descriptivo del reporte (Detallado o Resumen).</li>
     *   <li>Marca de tiempo precisa con el momento exacto de la exportación.</li>
     * </ol>
     *
     * @param document El objeto {@link Document} de iText en proceso de construcción.
     * @param empresa Nombre de la organización a mostrar.
     * @param subtitulo Texto secundario que describe el tipo de reporte y filtros.
     * @throws DocumentException Si se produce un error al intentar añadir elementos al flujo del documento.
     */
    private void agregarCabeceras(Document document, String empresa, String subtitulo) throws DocumentException {
        Paragraph pEmpresa = new Paragraph(empresa != null ? empresa.toUpperCase() : "EMPRESA", FUENTE_TITULO);
        pEmpresa.setAlignment(Element.ALIGN_CENTER);
        document.add(pEmpresa);

        Paragraph pSub = new Paragraph(subtitulo, FUENTE_SUBTITULO);
        pSub.setAlignment(Element.ALIGN_CENTER);
        document.add(pSub);

        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Paragraph pFecha = new Paragraph("Generado el: " + fechaActual, FUENTE_FECHA_GEN);
        pFecha.setAlignment(Element.ALIGN_CENTER);
        pFecha.setSpacingAfter(10);
        document.add(pFecha);
    }

    /**
     * Configura el pie de página automático para todas las hojas del documento.
     * Incluye el nombre de la aplicación y la numeración de página.
     *
     * @param document El objeto Documento al que se le aplicará el pie de página.
     */
    private void configurarPieDePagina(Document document) {
        Font fontPie = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Phrase pieText = new Phrase("Generado por GestorRH App  |  Página ", fontPie);
        HeaderFooter footer = new HeaderFooter(pieText, true);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setBorder(Rectangle.TOP);
        footer.setBorderColor(Color.LIGHT_GRAY);
        document.setFooter(footer);
    }

    /**
     * Crea y añade una celda de estilo cabecera a una tabla.
     * Utiliza el color corporativo y texto en blanco con alineación centrada.
     *
     * @param table La tabla de iText a la que se añadirá la celda.
     * @param texto El contenido textual de la cabecera.
     * @param colorFondo El color de fondo para la celda.
     */
    private void agregarCeldaCabecera(PdfPTable table, String texto, Color colorFondo) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FUENTE_CABECERA));
        cell.setBackgroundColor(colorFondo);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderColor(Color.WHITE);
        table.addCell(cell);
    }

    /**
     * Crea y añade una celda de datos estándar a la tabla.
     *
     * @param table La tabla de destino.
     * @param texto El valor a insertar en la celda.
     */
    private void agregarCeldaDato(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", FUENTE_CELDA));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(GRIS_CLARO);
        table.addCell(cell);
    }

    /**
     * Añade una fila final a la tabla de detalle con los sumatorios generales de tiempo.
     *
     * @param table Tabla de iText en construcción.
     * @param totalMinutos Suma total de minutos reales trabajados.
     * @param totalExtras Suma total de minutos extra generados.
     */
    private void agregarFilaTotalDetalle(PdfPTable table, long totalMinutos, long totalExtras) {
        PdfPCell cellTexto = new PdfPCell(new Phrase("TOTAL GENERAL DE ESTE REPORTE", FUENTE_CABECERA));
        cellTexto.setColspan(4);
        cellTexto.setBackgroundColor(COLOR_TOTALES);
        cellTexto.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellTexto.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellTexto.setPadding(8);
        cellTexto.setBorderColor(Color.WHITE);
        table.addCell(cellTexto);

        agregarCeldaCabecera(table, formatearMinutos(totalMinutos), COLOR_TOTALES);
        agregarCeldaCabecera(table, formatearMinutos(totalExtras), COLOR_TOTALES);
    }

    /**
     * Añade una fila de sumatorios finales a la tabla de resumen.
     *
     * @param table Tabla de destino.
     * @param totalDias Suma total de días trabajados reportados.
     * @param totalMinutos Suma total de minutos reales.
     * @param totalExtras Suma total de minutos extra.
     */
    private void agregarFilaTotalResumen(PdfPTable table, long totalDias, long totalMinutos, long totalExtras) {
        PdfPCell cellTexto = new PdfPCell(new Phrase("TOTALES", FUENTE_CABECERA));
        cellTexto.setColspan(2);
        cellTexto.setBackgroundColor(COLOR_TOTALES);
        cellTexto.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellTexto.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellTexto.setPadding(8);
        cellTexto.setBorderColor(Color.WHITE);
        table.addCell(cellTexto);

        agregarCeldaCabecera(table, String.valueOf(totalDias), COLOR_TOTALES);
        agregarCeldaCabecera(table, formatearMinutos(totalMinutos), COLOR_TOTALES);
        agregarCeldaCabecera(table, formatearMinutos(totalExtras), COLOR_TOTALES);
    }

    /**
     * Utilidad para convertir una cantidad de minutos en un formato legible de horas y minutos.
     * Ejemplo: 125 -> "2h 5min"
     *
     * @param minutosTotales Cantidad de minutos a formatear.
     * @return String Cadena de texto formateada para el PDF.
     */
    private String formatearMinutos(Long minutosTotales) {
        if (minutosTotales == null || minutosTotales == 0) return "0h 0min";
        long h = minutosTotales / 60;
        long m = minutosTotales % 60;
        if (h == 0) return m + "min";
        return h + "h " + m + "min";
    }
}
