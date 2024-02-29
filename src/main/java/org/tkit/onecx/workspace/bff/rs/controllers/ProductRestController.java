package org.tkit.onecx.workspace.bff.rs.controllers;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.workspace.bff.rs.mappers.*;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.product.store.client.api.ProductsApi;
import gen.org.tkit.onecx.product.store.client.model.ProductItemPageResult;
import gen.org.tkit.onecx.workspace.bff.rs.internal.ProductApiService;
import gen.org.tkit.onecx.workspace.bff.rs.internal.model.*;
import gen.org.tkit.onecx.workspace.client.api.ProductInternalApi;
import gen.org.tkit.onecx.workspace.client.model.*;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class ProductRestController implements ProductApiService {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    ProductMapper productMapper;

    @Inject
    @RestClient
    ProductInternalApi productClient;

    @Inject
    @RestClient
    ProductsApi productStoreClient;

    @Override
    public Response createProductInWorkspace(String id, CreateProductRequestDTO createProductRequestDTO) {
        CreateProductRequest request = productMapper.map(createProductRequestDTO, id);
        try (Response response = productClient.createProduct(request)) {
            CreateUpdateProductResponseDTO createdProduct = productMapper
                    .mapToCreateUpdate(productMapper.map(response.readEntity(Product.class)));
            return Response.status(response.getStatus()).entity(createdProduct).build();
        }
    }

    @Override
    public Response deleteProductById(String id, String productId) {
        try (Response response = productClient.deleteProductById(productId)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getProductsForWorkspaceId(String id) {
        var criteria = new ProductSearchCriteria().workspaceId(id);
        try (Response response = productClient.searchProducts(criteria)) {
            List<ProductDTO> productList = productMapper
                    .mapProductListToDTOs(response.readEntity(ProductPageResult.class));
            return Response.status(response.getStatus()).entity(productList).build();
        }
    }

    @Override
    public Response searchAvailableProducts(ProductStoreSearchCriteriaDTO productStoreSearchCriteriaDTO) {
        try (Response response = productStoreClient
                .searchProductsByCriteria(productMapper.map(productStoreSearchCriteriaDTO))) {
            ProductStorePageResultDTO availableProducts = productMapper.map(response.readEntity(ProductItemPageResult.class));
            return Response.status(response.getStatus()).entity(availableProducts).build();
        }
    }

    @Override
    public Response updateProductById(String id, String productId, UpdateProductRequestDTO updateProductRequestDTO) {
        try (Response response = productClient.updateProductById(productId, productMapper.map(updateProductRequestDTO))) {
            CreateUpdateProductResponseDTO updateProductResponseDTO = productMapper
                    .mapToCreateUpdate(productMapper.map(response.readEntity(Product.class)));
            return Response.status(response.getStatus()).entity(updateProductResponseDTO).build();
        }
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

    @ServerExceptionMapper
    public Response restException(WebApplicationException ex) {
        return Response.status(ex.getResponse().getStatus()).build();
    }

}
