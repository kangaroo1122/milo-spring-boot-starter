package com.coctrl.milo.model;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @desc
 * @since 2020/4/13
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class HistoryRead {
    private String identifier;

    private List<Object> value;
}
