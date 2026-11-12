import React from "react";
import { Pressable, Text, View } from "react-native";
import { router } from "expo-router";
import { useOwner } from "../../OwnerContext";
import { Screen, Card, Button, PetAvatar, styles } from "../../ui";
import {
  SPECIES,
  dateLabel,
} from "../../../../../packages/owner-domain/src/model.js";
export default function Pets() {
  const { pets, selectPet } = useOwner();
  return (
    <Screen
      title="Your little family."
      subtitle="Their details. Their stories. Their care."
    >
      {pets.map((p) => (
        <Card key={p.petId}>
          <View style={styles.between}>
            <View style={styles.row}>
              <PetAvatar pet={p} />
              <View>
                <Text style={styles.heading}>{p.name}</Text>
                <Text style={styles.body}>
                  {SPECIES[p.species] || p.species} ·{" "}
                  {p.breed || "Breed not recorded"}
                </Text>
              </View>
            </View>
            <Pressable
              onPress={() =>
                router.push({ pathname: "/pet-form", params: { id: p.petId } })
              }
            >
              <Text style={styles.link}>Edit</Text>
            </Pressable>
          </View>
          <View style={styles.divider} />
          <Text style={styles.body}>
            {p.age || "Age not recorded"} · Birthday {dateLabel(p.dob)}
          </Text>
          <Button
            title="Health passport →"
            secondary
            onPress={() => {
              selectPet(p.petId);
              router.push("/passport");
            }}
          />
        </Card>
      ))}
      <Button title="+ Add a pet" onPress={() => router.push("/pet-form")} />
    </Screen>
  );
}
