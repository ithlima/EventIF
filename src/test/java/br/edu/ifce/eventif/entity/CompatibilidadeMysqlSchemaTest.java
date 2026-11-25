package br.edu.ifce.eventif.entity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que nenhuma tabela ou coluna gerada pelo mapeamento JPA use palavra reservada do MySQL 8.x.
 *
 * <p>O H2 aceita identificadores como {@code LOCAL}, mas o MySQL rejeita o DDL e as consultas,
 * impedindo a subida em Docker (ADR-14). Lista curada de palavras reservadas do MySQL 8.x.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class CompatibilidadeMysqlSchemaTest {

    private static final Set<String> PALAVRAS_RESERVADAS_MYSQL = Set.of(
        "ACCESSIBLE", "ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "BEFORE", "BETWEEN", "BY",
        "CALL", "CASCADE", "CASE", "CHANGE", "CHECK", "COLUMN", "CONDITION", "CONSTRAINT", "CONVERT",
        "CREATE", "CROSS", "CUBE", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_USER", "CURSOR", "DATABASE",
        "DEFAULT", "DELETE", "DESC", "DESCRIBE", "DISTINCT", "DIV", "DROP", "EACH", "ELSE", "EMPTY",
        "EXISTS", "EXPLAIN", "FALSE", "FETCH", "FOR", "FOREIGN", "FROM", "FUNCTION", "GROUP", "GROUPS",
        "HAVING", "IF", "IN", "INDEX", "INNER", "INSERT", "INTERVAL", "INTO", "IS", "JOIN", "KEY", "KEYS",
        "LAG", "LEAD", "LEFT", "LIKE", "LIMIT", "LINES", "LOAD", "LOCAL", "LOCALTIME", "LOCALTIMESTAMP",
        "LOCK", "MATCH", "MOD", "NATURAL", "NOT", "NULL", "OF", "ON", "OPTION", "OR", "ORDER", "OUT",
        "OUTER", "OVER", "PARTITION", "PRIMARY", "RANGE", "RANK", "READ", "REFERENCES", "RELEASE",
        "RENAME", "REPEAT", "REPLACE", "REQUIRE", "RESIGNAL", "RESTRICT", "RETURN", "REVOKE", "RIGHT",
        "ROW", "ROWS", "SCHEMA", "SELECT", "SET", "SHOW", "SIGNAL", "SYSTEM", "TABLE", "THEN", "TO",
        "TRIGGER", "TRUE", "UNION", "UNIQUE", "UPDATE", "USAGE", "USE", "USING", "VALUES", "WHEN",
        "WHERE", "WINDOW", "WITH", "WRITE"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void tabelasEColunasNaoDevemUsarPalavrasReservadasDoMysql() {
        List<Map<String, Object>> colunas = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC'"
        );

        assertThat(colunas).isNotEmpty();

        for (Map<String, Object> coluna : colunas) {
            String tabela = String.valueOf(coluna.get("TABLE_NAME")).toUpperCase();
            String nomeColuna = String.valueOf(coluna.get("COLUMN_NAME")).toUpperCase();

            assertThat(PALAVRAS_RESERVADAS_MYSQL)
                .as("Tabela %s usa palavra reservada do MySQL", tabela)
                .doesNotContain(tabela);
            assertThat(PALAVRAS_RESERVADAS_MYSQL)
                .as("Coluna %s.%s usa palavra reservada do MySQL", tabela, nomeColuna)
                .doesNotContain(nomeColuna);
        }

        assertThat(colunas)
            .anySatisfy(coluna -> {
                assertThat(String.valueOf(coluna.get("TABLE_NAME"))).isEqualToIgnoringCase("eventos");
                assertThat(String.valueOf(coluna.get("COLUMN_NAME"))).isEqualToIgnoringCase("local_evento");
            });
    }
}
