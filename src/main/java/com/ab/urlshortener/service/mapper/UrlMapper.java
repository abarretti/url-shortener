package com.ab.urlshortener.service.mapper;

import com.ab.urlshortener.controller.response.UrlResponse;
import com.ab.urlshortener.entity.Url;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UrlMapper {
    UrlResponse toResponse(Url url);
}
