import React, { useEffect, useState } from "react";
import { ActivityIndicator, Pressable, Text, View } from "react-native";
import { router, Redirect } from "expo-router";
import { useOwner, type Data } from "../OwnerContext";
import {
  Screen,
  Card,
  Input,
  Button,
  ErrorNotice,
  styles,
  color,
} from "../ui";
import { ownerApi } from "../api";
import { todayIST, availableFutureSlots } from "../../../../packages/owner-domain/src/model.js";
export default function Book() {
  const { user, pets, pet, reload } = useOwner();
  const [vets, setVets] = useState<Data[]>([]),
    [slotResult, setSlotResult] = useState<{key:string;items:Data[]}>({key:"",items:[]}),
    [loading, setLoading] = useState(true),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const [form, setForm] = useState({
    petId: pet?.petId || "",
    vetUserId: "",
    hospitalId: "",
    date: todayIST(),
    slotId: "",
    reason: "",
  });
  useEffect(() => {
    let active = true;
    ownerApi
      .vets()
      .then((v: Data[]) => {
        if (active) setVets(v);
      })
      .catch((e: Error) => {
        if (active) setError(e.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);
  const slotKey = `${form.vetUserId}:${form.hospitalId}:${form.date}`;
  const validDate = /^\d{4}-\d{2}-\d{2}$/.test(form.date);
  const slots = slotResult.key === slotKey ? slotResult.items : [];
  const checking = Boolean(form.vetUserId && validDate && slotResult.key !== slotKey);
  useEffect(() => {
    if (!form.vetUserId || !validDate) return;
    let active = true;
    ownerApi.slots(form.vetUserId,form.date).then((value:Data[]) => {
      if(active) setSlotResult({key:slotKey,items:availableFutureSlots(value,form.hospitalId)});
    }).catch((error:Error) => {
      if(active) {setError(error.message);setSlotResult({key:slotKey,items:[]});}
    });
    return () => { active=false; };
  }, [form.vetUserId,form.hospitalId,form.date,validDate,slotKey]);
  if (!user) return <Redirect href="/sign-in" />;
  async function book() {
    setError("");
    if (!form.petId || !form.slotId) {
      setError("Choose a pet and an available slot.");
      return;
    }
    if (form.reason.trim().length < 3) {
      setError("Describe the reason for your visit (at least 3 characters).");
      return;
    }
    setBusy(true);
    try {
      await ownerApi.book({
        petId: form.petId,
        slotId: form.slotId,
        reason: form.reason.trim(),
      });
      await reload();
      router.replace("/(tabs)/visits");
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : "Unable to book. The slot may no longer be available.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <Screen
      title="A date for their wellbeing."
      subtitle="Choose a real appointment slot from your care team."
    >
      {error && <ErrorNotice message={error} />}
      <Card>
        <Text style={styles.label}>Who is this visit for?</Text>
        <View style={[styles.row, { flexWrap: "wrap" }]}>
          {pets.map((p) => (
            <Pressable
              key={p.petId}
              style={[styles.pill, form.petId === p.petId && styles.pillActive]}
              onPress={() => setForm({ ...form, petId: p.petId })}
            >
              <Text style={styles.body}>{p.name}</Text>
            </Pressable>
          ))}
        </View>
        {!pets.length && (
          <Button
            title="Add a pet first"
            secondary
            onPress={() => router.replace("/pet-form")}
          />
        )}
        <Text style={styles.label}>Choose your veterinarian and clinic</Text>
        {loading && <ActivityIndicator color={color.teal} />}
        <View style={{ gap: 10 }}>
          {vets.flatMap((v) =>
            (v.hospitals || []).map((h: Data) => (
              <Pressable
                key={`${v.vetUserId}-${h.hospitalId}`}
                style={[
                  styles.pill,
                  form.vetUserId === v.vetUserId &&
                    form.hospitalId === h.hospitalId &&
                    styles.pillActive,
                ]}
                onPress={() =>
                  setForm({
                    ...form,
                    vetUserId: v.vetUserId,
                    hospitalId: h.hospitalId,
                    slotId: "",
                  })
                }
              >
                <Text style={styles.heading}>
                  Dr. {v.firstName} {v.lastName}
                </Text>
                <Text style={styles.small}>
                  {h.name} · {h.city}
                </Text>
              </Pressable>
            )),
          )}
        </View>
        <Input
          label="Visit date · IST (YYYY-MM-DD)"
          value={form.date}
          maxLength={10}
          onChangeText={(date) => setForm({ ...form, date, slotId: "" })}
        />
        <Text style={styles.label}>Available slots · IST</Text>
        {checking ? (
          <ActivityIndicator color={color.teal} />
        ) : (
          <View style={[styles.row, { flexWrap: "wrap" }]}>
            {slots.map((s) => (
              <Pressable
                key={s.slotId}
                style={[
                  styles.pill,
                  form.slotId === s.slotId && styles.pillActive,
                ]}
                onPress={() => setForm({ ...form, slotId: s.slotId })}
              >
                <Text style={styles.small}>
                  {s.startTime?.slice(0, 5)}–{s.endTime?.slice(0, 5)}
                </Text>
              </Pressable>
            ))}
          </View>
        )}
        {!checking && !slots.length && (
          <Text style={styles.body}>
            {form.vetUserId
              ? "No available slots returned. Try another date or contact the clinic."
              : "Choose a veterinarian to see availability."}
          </Text>
        )}
        <Input
          label="Reason for the visit"
          value={form.reason}
          multiline
          maxLength={500}
          onChangeText={(reason) => setForm({ ...form, reason })}
        />
        <Button
          title="Confirm appointment"
          busy={busy}
          disabled={!form.slotId || !form.petId}
          onPress={book}
        />
      </Card>
    </Screen>
  );
}
