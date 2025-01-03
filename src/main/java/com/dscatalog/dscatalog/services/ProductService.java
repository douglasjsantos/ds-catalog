package com.dscatalog.dscatalog.services;

import com.dscatalog.dscatalog.dtos.CategoryDTO;
import com.dscatalog.dscatalog.dtos.ProductDTO;
import com.dscatalog.dscatalog.exceptions.DatabaseException;
import com.dscatalog.dscatalog.exceptions.EntityNotFoundException;
import com.dscatalog.dscatalog.models.CategoryModel;
import com.dscatalog.dscatalog.models.ProductModel;
import com.dscatalog.dscatalog.projections.ProductProjection;
import com.dscatalog.dscatalog.repositories.CategoryRepository;
import com.dscatalog.dscatalog.repositories.ProductRepository;
import com.dscatalog.dscatalog.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private CategoryRepository categoryRepository;


    @Transactional(readOnly = true)
    public Page<ProductDTO> listAllPaged(Pageable pageable){

        Page<ProductModel> productList = repository.findAll(pageable);

        return productList.map(x -> new ProductDTO(x));
    }

    @Transactional(readOnly = true)
    public ProductDTO findById(Long id){

        Optional<ProductModel> obj = repository.findById(id);
        ProductModel entity = obj.orElseThrow(() -> new EntityNotFoundException("Entity not found"));

        return new ProductDTO(entity, entity.getCategories());

    }

    @Transactional
    public ProductDTO save(ProductDTO productDTO) {

        ProductModel productModel = new ProductModel();
        productModel.setName(productDTO.getName());
        productModel.setDescription(productDTO.getDescription());
        productModel.setDate(productDTO.getDate());
        productModel.setPrice(productDTO.getPrice());
        productModel.setImgUrl(productDTO.getImgUrl());

        productDTO.getCategories().forEach(catDTO -> {
            CategoryModel category = categoryRepository.findById(catDTO.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + catDTO.getId()));
            productModel.getCategories().add(category);
        });

        ProductModel savedModel = repository.save(productModel);

        return new ProductDTO(savedModel, savedModel.getCategories());

    }

    @Transactional
    public ProductDTO update(Long id, ProductDTO productDTO) {

        try{
            ProductModel productModel = repository.getReferenceById(id);
            productModel.setName(productDTO.getName());
            productModel.setDescription(productDTO.getDescription());
            productModel.setDate(productDTO.getDate());
            productModel.setPrice(productDTO.getPrice());
            productModel.setImgUrl(productDTO.getImgUrl());
            productModel = repository.save(productModel);
            return new ProductDTO(productModel);
        } catch (EntityNotFoundException e) {
            throw new EntityNotFoundException("Id not found" + id);
        }

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void delete(Long id) {

        if(!repository.existsById(id)){
            throw new EntityNotFoundException("Entity not found");
        }
        try {
            repository.deleteById(id);

        }catch (DataIntegrityViolationException e){
            throw new DatabaseException("Integrity violation");
        }
    }


    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Page<ProductDTO> findAllPaged(String name, String categoryId, Pageable pageable) {


        List<Long> categoryIds = Arrays.asList();
        if(!"0".equals(categoryId)){
            categoryIds = Arrays.asList(categoryId.split(",")).stream().map(Long::parseLong).toList();
        }

        Page<ProductProjection> page = repository.searchProducts(categoryIds, name, pageable);
        List<Long> productIds = page.map(x -> x.getId()).toList();

        List<ProductModel> entities = repository.searchProductsWithCategories(productIds);
        entities = (List<ProductModel>) Utils.replace(page.getContent(), entities);

        List<ProductDTO> dtos = entities.stream().map(p -> new ProductDTO(p, p.getCategories())).toList();


        return new PageImpl<>(dtos,page.getPageable(),page.getTotalPages());
    }
}