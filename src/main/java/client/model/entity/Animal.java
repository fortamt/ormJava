package client.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import orm.annotation.Entity;
import orm.annotation.Id;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Animal {
    @Id
    Long id;
    @NonNull String name;
}
