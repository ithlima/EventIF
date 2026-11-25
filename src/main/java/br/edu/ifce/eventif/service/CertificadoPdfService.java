package br.edu.ifce.eventif.service;

import br.edu.ifce.eventif.dto.CertificadoAtividadeView;
import br.edu.ifce.eventif.dto.CertificadoView;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * Gerador dinâmico de certificados em PDF (OpenPDF) a partir de {@link CertificadoView} (EX03).
 */
@Service
public class CertificadoPdfService {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color VERDE_IFCE = new Color(20, 108, 67);
    private static final Color CINZA_TEXTO = new Color(33, 37, 41);
    private static final Color CINZA_SUAVE = new Color(108, 117, 125);
    private static final Color FUNDO_CABECALHO_TABELA = new Color(240, 247, 243);
    private static final Color BORDA_TABELA = new Color(210, 224, 216);

    public byte[] gerarPdf(CertificadoView certificado) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 48, 48, 42, 42);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fonteSelo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, VERDE_IFCE);
            Font fonteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, VERDE_IFCE);
            Font fonteCorpo = FontFactory.getFont(FontFactory.HELVETICA, 12, CINZA_TEXTO);
            Font fonteDestaque = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, CINZA_TEXTO);
            Font fonteCabecalhoTabela = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, VERDE_IFCE);
            Font fonteCelula = FontFactory.getFont(FontFactory.HELVETICA, 10, CINZA_TEXTO);
            Font fonteRodape = FontFactory.getFont(FontFactory.HELVETICA, 9, CINZA_SUAVE);

            Paragraph selo = new Paragraph(
                "Instituto Federal de Educação, Ciência e Tecnologia — EventIF",
                fonteSelo
            );
            selo.setAlignment(Element.ALIGN_CENTER);
            selo.setSpacingAfter(6);
            document.add(selo);

            Paragraph titulo = new Paragraph("CERTIFICADO DE PARTICIPAÇÃO", fonteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(18);
            document.add(titulo);

            Paragraph corpo = new Paragraph();
            corpo.setAlignment(Element.ALIGN_CENTER);
            corpo.setLeading(18);
            corpo.setSpacingAfter(20);
            corpo.add(new Phrase("Certificamos que ", fonteCorpo));
            corpo.add(new Phrase(certificado.participanteNome(), fonteDestaque));
            corpo.add(new Phrase(" participou do evento ", fonteCorpo));
            corpo.add(new Phrase(certificado.eventoTitulo(), fonteDestaque));
            corpo.add(new Phrase(", realizado em " + certificado.eventoLocal(), fonteCorpo));
            corpo.add(new Phrase(
                ", no período de " + certificado.eventoDataInicio().format(FORMATO_DATA)
                    + " a " + certificado.eventoDataFim().format(FORMATO_DATA)
                    + ", perfazendo a carga horária total de ",
                fonteCorpo
            ));
            corpo.add(new Phrase(certificado.cargaHorariaTotal() + " horas", fonteDestaque));
            corpo.add(new Phrase(" nas atividades abaixo com presença confirmada:", fonteCorpo));
            document.add(corpo);

            PdfPTable tabela = new PdfPTable(new float[]{4.2f, 1.5f, 2.8f, 1.3f});
            tabela.setWidthPercentage(100);
            tabela.setSpacingAfter(20);

            adicionarCelulaCabecalho(tabela, "Atividade", fonteCabecalhoTabela, Element.ALIGN_LEFT);
            adicionarCelulaCabecalho(tabela, "Tipo", fonteCabecalhoTabela, Element.ALIGN_LEFT);
            adicionarCelulaCabecalho(tabela, "Horário", fonteCabecalhoTabela, Element.ALIGN_LEFT);
            adicionarCelulaCabecalho(tabela, "Carga horária", fonteCabecalhoTabela, Element.ALIGN_RIGHT);

            for (CertificadoAtividadeView atv : certificado.atividadesPresentes()) {
                adicionarCelulaCorpo(tabela, atv.titulo(), fonteCelula, Element.ALIGN_LEFT);
                adicionarCelulaCorpo(tabela, atv.tipo().getDescricao(), fonteCelula, Element.ALIGN_LEFT);
                String horario = atv.inicio().format(FORMATO_DATA_HORA) + " a " + atv.fim().format(FORMATO_DATA_HORA);
                adicionarCelulaCorpo(tabela, horario, fonteCelula, Element.ALIGN_LEFT);
                adicionarCelulaCorpo(tabela, atv.cargaHoraria() + "h", fonteCelula, Element.ALIGN_RIGHT);
            }

            PdfPCell totalLabel = new PdfPCell(new Phrase("Carga horária total certificada:", fonteCabecalhoTabela));
            totalLabel.setColspan(3);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabel.setPadding(8);
            totalLabel.setBackgroundColor(FUNDO_CABECALHO_TABELA);
            totalLabel.setBorderColor(BORDA_TABELA);
            tabela.addCell(totalLabel);

            PdfPCell totalValor = new PdfPCell(new Phrase(certificado.cargaHorariaTotal() + "h", fonteCabecalhoTabela));
            totalValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValor.setPadding(8);
            totalValor.setBackgroundColor(FUNDO_CABECALHO_TABELA);
            totalValor.setBorderColor(BORDA_TABELA);
            tabela.addCell(totalValor);

            document.add(tabela);

            Paragraph rodape = new Paragraph(
                "Documento gerado dinamicamente pelo sistema EventIF · Participante: "
                    + certificado.participanteEmail(),
                fonteRodape
            );
            rodape.setAlignment(Element.ALIGN_CENTER);
            document.add(rodape);
        } catch (DocumentException ex) {
            throw new IllegalStateException("Falha ao gerar o arquivo PDF do certificado.", ex);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void adicionarCelulaCabecalho(PdfPTable tabela, String texto, Font fonte, int alinhamento) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fonte));
        cell.setHorizontalAlignment(alinhamento);
        cell.setPadding(8);
        cell.setBackgroundColor(FUNDO_CABECALHO_TABELA);
        cell.setBorderColor(BORDA_TABELA);
        tabela.addCell(cell);
    }

    private void adicionarCelulaCorpo(PdfPTable tabela, String texto, Font fonte, int alinhamento) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fonte));
        cell.setHorizontalAlignment(alinhamento);
        cell.setPadding(7);
        cell.setBorderColor(BORDA_TABELA);
        tabela.addCell(cell);
    }
}
