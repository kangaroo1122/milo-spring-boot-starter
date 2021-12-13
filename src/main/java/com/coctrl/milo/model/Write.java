package com.coctrl.milo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @since 2020/4/13
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Write {
    private String identifier;
    private Integer value;
}
