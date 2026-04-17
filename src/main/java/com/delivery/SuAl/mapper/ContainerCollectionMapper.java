package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Admin;
import com.delivery.SuAl.entity.ContainerCollection;
import com.delivery.SuAl.model.response.warehouse.ContainerCollectionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ContainerCollectionMapper {

    @Mapping(target = "collectionId", source = "containerCollection.id")
    @Mapping(target = "warehouseId", source = "containerCollection.warehouse.id")
    @Mapping(target = "warehouseName", source = "containerCollection.warehouse.name")
    @Mapping(target = "productId", source = "containerCollection.product.id")
    @Mapping(target = "productName", source = "containerCollection.product.name")
    @Mapping(target = "emptyContainersCollected", source = "containerCollection.emptyContainers")
    @Mapping(target = "damagedContainersCollected", source = "containerCollection.damagedContainers")
    @Mapping(target = "totalCollected", source = "containerCollection.totalCollected")
    @Mapping(target = "collectionDateTime", source = "containerCollection.collectionDateTime")
    @Mapping(target = "notes", source = "containerCollection.notes")
    @Mapping(target = "collectedBy", source = "admin")
    ContainerCollectionResponse toResponse(ContainerCollection containerCollection, Admin admin);

    default String map(Admin admin) {
        if (admin == null) {
            return "Unknown";
        }
        return admin.getFirstName() + " " + admin.getLastName();
    }
}
