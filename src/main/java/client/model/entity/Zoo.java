package client.model.entity;

import lombok.*;
import orm.annotation.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@Table(name = "Zoo")
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
public class Zoo {
    @Id(name = "id")
    Long id;

    @Column(name = "name")
    @NonNull
    String name;

    @ToString.Exclude
    @OneToMany(mappedBy = "zoo")
    List<Animal> animals = new ArrayList<>();

    public void addAnimal(Animal animal) {
        this.getAnimals().add(animal);
        animal.setZoo(this);
    }

    public void removeAnimal(Animal animal) {
        getAnimals().remove(animal);
        animal.setZoo(null);
    }


    @Override
    public int hashCode() {
        return 10;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Zoo other = (Zoo) obj;
        return id != null && id.equals(other.getId());
    }
}
