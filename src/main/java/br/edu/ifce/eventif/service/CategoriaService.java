package br.edu.ifce.eventif.service;

import br.edu.ifce.eventif.dto.CategoriaForm;
import br.edu.ifce.eventif.entity.Categoria;
import br.edu.ifce.eventif.exception.RecursoNaoEncontradoException;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.repository.CategoriaRepository;
import br.edu.ifce.eventif.repository.EventoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de gerenciamento de categorias pelo administrador (RF03).
 */
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final EventoRepository eventoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, EventoRepository eventoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarTodas() {
        return categoriaRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }

    @Transactional
    public Categoria criar(CategoriaForm form) {
        String nomeNormalizado = form.getNome().trim();
        if (categoriaRepository.existsByNomeIgnoreCase(nomeNormalizado)) {
            throw new RegraNegocioException("Já existe uma categoria cadastrada com este nome.");
        }
        return categoriaRepository.save(new Categoria(nomeNormalizado));
    }

    @Transactional
    public Categoria atualizar(Long id, CategoriaForm form) {
        Categoria categoria = buscarPorId(id);
        String nomeNormalizado = form.getNome().trim();

        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(nomeNormalizado, id)) {
            throw new RegraNegocioException("Já existe outra categoria cadastrada com este nome.");
        }

        categoria.setNome(nomeNormalizado);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscarPorId(id);
        if (eventoRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Não é possível excluir uma categoria vinculada a eventos.");
        }
        categoriaRepository.delete(categoria);
    }
}
