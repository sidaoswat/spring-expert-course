package io.github.sidaoswat.service.impl;

import io.github.sidaoswat.domain.entity.Cliente;
import io.github.sidaoswat.domain.entity.ItemPedido;
import io.github.sidaoswat.domain.entity.Pedido;
import io.github.sidaoswat.domain.entity.Produto;
import io.github.sidaoswat.domain.enums.StatusPedido;
import io.github.sidaoswat.domain.repository.Clientes;
import io.github.sidaoswat.domain.repository.ItensPedido;
import io.github.sidaoswat.domain.repository.Pedidos;
import io.github.sidaoswat.domain.repository.Produtos;
import io.github.sidaoswat.exception.PedidoNaoEncontradoException;
import io.github.sidaoswat.exception.RegraNegocioException;
import io.github.sidaoswat.rest.dto.ItemPedidoDTO;
import io.github.sidaoswat.rest.dto.PedidoDTO;
import io.github.sidaoswat.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
//@RequiredArgsConstructor
public class PedidoServiceImp implements PedidoService {

    @Autowired
    private Pedidos repository;

    @Autowired
    private Clientes clientesRepository;

    @Autowired
    private Produtos produtosRepository;

    @Autowired
    private ItensPedido itensPedidoRepository;

    @Override
    @Transactional
    public Pedido salvar(PedidoDTO dto) {
        Integer idCliente = dto.getCliente();
        Cliente cliente = clientesRepository
                .findById(idCliente)
                .orElseThrow(() -> new RegraNegocioException("Código de cliente inválido."));

        Pedido pedido = new Pedido();
        pedido.setTotal(dto.getTotal());
        pedido.setDataPedido(LocalDate.now());
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.REALIZADO);

        List<ItemPedido> itemPedidos = converterItems(pedido, dto.getItems());
        repository.save(pedido);
        itensPedidoRepository.saveAll(itemPedidos);
        pedido.setItens(itemPedidos);
        return pedido;
    }

    @Override
    public Optional<Pedido> obterPedidoCompleto(Integer id) {
        return repository.findByIdFetchItens(id);
    }

    @Override
    @Transactional
    public void atualizarStatus(Integer id, StatusPedido statusPedido) {
        repository.findById(id)
                .map(pedido -> {
                    pedido.setStatus(statusPedido);
                    return repository.save(pedido);
                }).orElseThrow(() -> new PedidoNaoEncontradoException());
    }

    private List<ItemPedido> converterItems(Pedido pedido, List<ItemPedidoDTO> items){
        if(items.isEmpty()){
            throw new RegraNegocioException("Não é possível realizar um pedido sem items");
        }
        return items
                .stream()
                .map( dto -> {
                    Integer idProduto = dto.getProduto();
                    Produto produto = produtosRepository
                            .findById(idProduto)
                            .orElseThrow(() -> new RegraNegocioException("Código de produto inválido: "+idProduto));

                    ItemPedido itemPedido = new ItemPedido();
                    itemPedido.setQuantidade(dto.getQuantidade());
                    itemPedido.setPedido(pedido);
                    itemPedido.setProduto(produto);
                    return itemPedido;
                }).collect(Collectors.toList());
    }
}
