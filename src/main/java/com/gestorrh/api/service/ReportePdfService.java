package com.gestorrh.api.service;

import com.gestorrh.api.dto.reporteDTO.ReporteDetalleDTO;
import com.gestorrh.api.dto.reporteDTO.ReporteResumenDTO;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
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
     * Genera el PDF detallado (Fila por fila, fichaje por fichaje) - DISEÑO REFINADO
     */
    public byte[] generarPdfDetalle(String nombreEmpresa, String subtituloFiltro, List<ReporteDetalleDTO> datos) {
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
            throw new RuntimeException("Error al generar el PDF de Detalle", e);
        }
    }

    /**
     * Genera el PDF resumido (Totales por empleado) - (SIN CAMBIOS, YA ESTABA BIEN)
     */
    public byte[] generarPdfResumen(String nombreEmpresa, String subtituloFiltro, List<ReporteResumenDTO> datos) {
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
            throw new RuntimeException("Error al generar el PDF de Resumen", e);
        }
    }

    // MÉTODOS PRIVADOS

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

    private void configurarPieDePagina(Document document) {
        Font fontPie = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Phrase pieText = new Phrase("Generado por GestorRH App  |  Página ", fontPie);
        HeaderFooter footer = new HeaderFooter(pieText, true);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setBorder(Rectangle.TOP);
        footer.setBorderColor(Color.LIGHT_GRAY);
        document.setFooter(footer);
    }

    private void agregarCeldaCabecera(PdfPTable table, String texto, Color colorFondo) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FUENTE_CABECERA));
        cell.setBackgroundColor(colorFondo);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderColor(Color.WHITE);
        table.addCell(cell);
    }

    private void agregarCeldaDato(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", FUENTE_CELDA));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(GRIS_CLARO);
        table.addCell(cell);
    }

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

    private String formatearMinutos(Long minutosTotales) {
        if (minutosTotales == null || minutosTotales == 0) return "0h 0min";
        long h = minutosTotales / 60;
        long m = minutosTotales % 60;
        if (h == 0) return m + "min";
        return h + "h " + m + "min";
    }
}
