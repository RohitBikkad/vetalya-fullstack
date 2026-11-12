import React from "react";
import { Pressable, Text, View } from "react-native";
import { router } from "expo-router";
import { useOwner } from "../../OwnerContext";
import {
  Screen,
  Card,
  Button,
  Empty,
  PetAvatar,
  PetSwitcher,
  styles,
  Icon,
} from "../../ui";
import {
  upcomingAppointments,
  dateLabel,
  timeLabel,
  SPECIES,
} from "../../../../../packages/owner-domain/src/model.js";
export default function Home() {
  const { user, profile, pets, appointments, due, pet, details } = useOwner();
  const visits = upcomingAppointments(appointments);
  const next = visits.find((a: any) => a.pet?.petId === pet?.petId);
  return (
    <Screen
      title={`Hello, ${profile?.firstName || user?.name?.split(" ")[0] || user?.fullName?.split(" ")[0] || "pet parent"}.`}
      subtitle="A little care. A happier life together."
    >
      <View style={styles.hero}>
        <Text style={[styles.eyebrow, { color: "#B9E4DC" }]}>
          THEIR HEALTH. YOUR PEACE OF MIND.
        </Text>
        <Text style={styles.heroTitle}>Every chapter,{"\n"}cared for.</Text>
        <Text style={[styles.body, { color: "#C6D6E5" }]}>
          Bring their everyday care into one thoughtful space.
        </Text>
        <Button
          title="Plan their next visit ↗"
          secondary
          onPress={() => router.push("/book")}
        />
      </View>
      <View style={styles.row}>
        {[
          [pets.length, "My pets"],
          [visits.length, "Visits ahead"],
          [due.length, "Care reminders"],
        ].map(([value, label]) => (
          <View style={styles.stat} key={label}>
            <Text style={styles.statNumber}>{value}</Text>
            <Text style={styles.small}>{label}</Text>
          </View>
        ))}
      </View>
      <Card>
        <View style={styles.between}>
          <Text style={styles.heading}>My companion</Text>
          <Pressable onPress={() => router.push("/(tabs)/pets")}>
            <Text style={styles.link}>View all ↗</Text>
          </Pressable>
        </View>
        <PetSwitcher />
        {pet ? (
          <>
            <View style={styles.row}>
              <PetAvatar pet={pet} />
              <View style={{ flex: 1, gap: 4 }}>
                <Text style={styles.heading}>{pet.name}</Text>
                <Text style={styles.body}>
                  {SPECIES[pet.species] || pet.species} ·{" "}
                  {pet.breed || "Breed not recorded"}
                </Text>
                <Text style={styles.small}>
                  {pet.age || "Age not recorded"}
                </Text>
              </View>
            </View>
            <Button
              title="Open health passport →"
              secondary
              onPress={() => router.push("/passport")}
            />
          </>
        ) : (
          <Empty
            title="Meet your companion"
            description="Add your first pet and begin their health story."
          />
        )}
      </Card>
      <Card>
        <View style={styles.row}>
          <Icon name="calendar" />
          <Text style={styles.heading}>Your next visit</Text>
        </View>
        {next ? (
          <>
            <Text style={styles.heading}>
              {next.hospital?.name || "Veterinary appointment"}
            </Text>
            <Text style={styles.body}>
              {next.pet?.name} · {dateLabel(next.appointmentAt)} ·{" "}
              {timeLabel(next.appointmentAt)}
            </Text>
            <Button
              title="View appointments"
              secondary
              onPress={() => router.push("/(tabs)/visits")}
            />
          </>
        ) : (
          <Empty
            title="A little space in the calendar"
            description="Your upcoming appointment details will appear here."
          />
        )}
      </Card>
      <Card>
        <View style={styles.row}>
          <Icon name="file-text" />
          <Text style={styles.heading}>Their whole health story.</Text>
        </View>
        <Text style={styles.body}>
          {(details.timeline || []).length} recorded events for{" "}
          {pet?.name || "your selected pet"}.
        </Text>
        <Button
          title="Explore health records ↗"
          secondary
          onPress={() => router.push("/(tabs)/records")}
        />
      </Card>
      <Card><Text style={styles.heading}>Care beyond the clinic.</Text><Text style={styles.body}>Discover planned grooming, walking, boarding, training and insurance integrations.</Text><Button title="Explore services →" secondary onPress={()=>router.push('/services')}/></Card>
    </Screen>
  );
}
