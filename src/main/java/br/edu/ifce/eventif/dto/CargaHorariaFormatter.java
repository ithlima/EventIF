package br.edu.ifce.eventif.dto;

import java.math.BigDecimal;

/**
 * Formata cargas horárias em horas exibindo inteiros sem casas decimais e decimais com vírgula (ex.: 4, 1,5).
 */
public final class CargaHorariaFormatter {

    private CargaHorariaFormatter() {
    }

    public static String formatar(BigDecimal valor) {
        if (valor == null) {
            return "0";
        }
        BigDecimal normalizado = valor.stripTrailingZeros();
        if (normalizado.scale() <= 0) {
            return normalizado.toPlainString();
        }
        return normalizado.toPlainString().replace('.', ',');
    }
}
