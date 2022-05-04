package client.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import orm.annotation.Entity;
import orm.annotation.Id;


@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
public class Zoo {
    @Id
    Long id;

    @NonNull
    String name;
}
