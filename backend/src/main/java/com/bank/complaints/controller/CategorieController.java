package com.bank.complaints.controller;

import com.bank.complaints.model.Categorie;
import com.bank.complaints.repository.CategorieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/categories")
public class CategorieController {

    @Autowired
    CategorieRepository categorieRepository;

    @GetMapping
    public ResponseEntity<List<Categorie>> getAllCategories() {
        return ResponseEntity.ok(categorieRepository.findAll());
    }
}
