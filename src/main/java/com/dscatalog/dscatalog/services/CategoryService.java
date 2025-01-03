package com.dscatalog.dscatalog.services;

import com.dscatalog.dscatalog.dtos.CategoryDTO;
import com.dscatalog.dscatalog.exceptions.DatabaseException;
import com.dscatalog.dscatalog.exceptions.EntityNotFoundException;
import com.dscatalog.dscatalog.models.CategoryModel;
import com.dscatalog.dscatalog.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository repository;

    @Transactional(readOnly = true)
    public Page<CategoryDTO> listAllPaged(Pageable pageable){

        Page<CategoryModel> categoryList = repository.findAll(pageable);

        return categoryList.map(categoryModel -> new CategoryDTO(categoryModel.getId(), categoryModel.getName()));
    }

    @Transactional(readOnly = true)
    public CategoryDTO findById(Long id){

        Optional<CategoryModel> obj = repository.findById(id);
        CategoryModel entity = obj.orElseThrow(() -> new EntityNotFoundException("Entity not found"));

        return new CategoryDTO(entity.getId(), entity.getName());

    }

    @Transactional
    public CategoryDTO save(CategoryDTO categoryDTO) {

        CategoryModel categoryModel = new CategoryModel();
        categoryModel.setName(categoryDTO.getName());
        CategoryModel savedModel = repository.save(categoryModel);
        return new CategoryDTO(savedModel.getId(), savedModel.getName());
    }

    @Transactional
    public CategoryDTO update(Long id, CategoryDTO categoryDTO) {

        try{
            CategoryModel categoryModel = repository.getReferenceById(id);
            categoryModel.setName(categoryDTO.getName());
            categoryModel = repository.save(categoryModel);
            return new CategoryDTO(categoryModel.getId(), categoryModel.getName());
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


}
