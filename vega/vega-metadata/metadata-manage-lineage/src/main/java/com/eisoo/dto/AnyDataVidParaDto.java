package com.eisoo.dto;

import lombok.Data;

import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/7 11:21
 * @Version:1.0
 */
@Data
public class AnyDataVidParaDto {
    //    {
//        "page": 10,
//            "size": 0,
//            "vids": [
//        "74a6fcc33ae3c1a47f7fd7c9fcc42c3c"
//  ]
//    }
    private Integer page = 1;
    private Integer size = 0;
    private List<String> vids;

    public AnyDataVidParaDto(List<String> vids) {
        this.vids = vids;
    }

}
