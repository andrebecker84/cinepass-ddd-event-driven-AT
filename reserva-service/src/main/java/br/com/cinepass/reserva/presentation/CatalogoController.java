package br.com.cinepass.reserva.presentation;

import br.com.cinepass.reserva.application.CatalogoQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CatalogoController {
    private final CatalogoQueryService service;
    public CatalogoController(CatalogoQueryService service){this.service=service;}

    @GetMapping("/api/filmes")
    public List<CatalogoQueryService.FilmeView> filmes(){return service.filmes();}

    @GetMapping("/api/sessoes")
    public List<CatalogoQueryService.SessaoView> sessoes(){return service.sessoes();}
}
