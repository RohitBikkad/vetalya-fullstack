import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  useRef,
} from "react";
import { ownerApi, client } from "./api";
export type Data = Record<string, any>;
type OwnerState = {
  user: Data | null;
  starting: boolean;
  loading: boolean;
  errors: string[];
  pets: Data[];
  appointments: Data[];
  due: Data[];
  profile: Data | null;
  selectedId: string;
  selectPet: (id: string) => void;
  pet: Data | undefined;
  details: Data;
  detailLoading: boolean;
  detailErrors: string[];
  reload: () => Promise<void>;
  reloadDetails: () => Promise<void>;
  signIn: (data: Data, register?: boolean) => Promise<void>;
  signOut: () => Promise<void>;
};
const Context = createContext<OwnerState | null>(null);
export function OwnerProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<Data | null>(null),
    [starting, setStarting] = useState(true),
    [loading, setLoading] = useState(false),
    [errors, setErrors] = useState<string[]>([]);
  const [pets, setPets] = useState<Data[]>([]),
    [appointments, setAppointments] = useState<Data[]>([]),
    [due, setDue] = useState<Data[]>([]),
    [profile, setProfile] = useState<Data | null>(null);
  const [selectedId, selectPet] = useState("");
  const [detailRefresh, setDetailRefresh] = useState(0);
  const [detailState, setDetailState] = useState<{
    key: string;
    data: Data;
    errors: string[];
  }>({ key: "", data: {}, errors: [] });
  const generation = useRef(0);
  const pet = pets.find((p) => p.petId === selectedId) || pets[0];
  const reload = useCallback(async () => {
    const version = generation.current;
    setLoading(true);
    setErrors([]);
    const results = await Promise.allSettled([
      ownerApi.pets(),
      ownerApi.appointments(),
      ownerApi.due(),
      ownerApi.profile(),
    ]);
    if (version !== generation.current) return;
    const failures: string[] = [];
    results.forEach((r, i) => {
      if (r.status === "fulfilled") {
        if (i === 0) setPets(r.value);
        if (i === 1) setAppointments(r.value);
        if (i === 2) setDue(r.value);
        if (i === 3) setProfile(r.value);
      } else
        failures.push(
          `${["Pets", "Appointments", "Care", "Profile"][i]}: ${r.reason.message}`,
        );
    });
    setErrors(failures);
    setLoading(false);
  }, []);
  const detailKey = `${pet?.petId || ""}:${detailRefresh}`;
  const details = detailState.key === detailKey ? detailState.data : {};
  const detailErrors = detailState.key === detailKey ? detailState.errors : [];
  const detailLoading = Boolean(pet && detailState.key !== detailKey);
  const reloadDetails = useCallback(async () => {
    setDetailRefresh((value) => value + 1);
  }, []);
  useEffect(() => {
    if (!pet?.petId) return;
    let active = true;
    const id = pet.petId;
    Promise.allSettled([
      ownerApi.passport(id),
      ownerApi.timeline(id),
      ownerApi.documents(id),
      ownerApi.vaccines(id),
      ownerApi.encounters(id),
      ownerApi.shares(id),
    ]).then((results) => {
      if (!active) return;
      const data: Data = {};
      const failures: string[] = [];
      results.forEach((result, index) => {
        const key = [
          "passport",
          "timeline",
          "documents",
          "vaccines",
          "encounters",
          "shares",
        ][index];
        if (result.status === "fulfilled") data[key] = result.value;
        else failures.push(`${key}: ${result.reason.message}`);
      });
      setDetailState({ key: detailKey, data, errors: failures });
    });
    return () => {
      active = false;
    };
  }, [pet?.petId, detailKey]);
  useEffect(() => {
    let active = true;
    (async () => {
      try {
        await client().refreshSession();
        const value = await client().request("/users/profile");
        if (value?.role !== "ROLE_PET_OWNER") {
          await client().logout();
          return;
        }
        if (active) {
          setUser(value);
          await reload();
        }
      } catch {
        /* Signed-out startup is expected; the sign-in screen also exposes configuration errors. */
      } finally {
        if (active) setStarting(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [reload]);
  async function signIn(data: Data, register = false) {
    const result = await (register
      ? client().register(data)
      : client().login(data));
    let value = result.user;
    if (!value) value = await client().request("/users/profile");
    if (value?.role !== "ROLE_PET_OWNER") {
      await client().logout();
      throw new Error(
        "This application is for pet-parent accounts. Use the staff web portal for your role.",
      );
    }
    generation.current++;
    setUser(value);
    await reload();
  }
  async function signOut() {
    generation.current++;
    try {
      await client().logout();
    } finally {
      setUser(null);
      setPets([]);
      setAppointments([]);
      setDue([]);
      setProfile(null);
      setDetailState({ key: "", data: {}, errors: [] });
      setErrors([]);
      setLoading(false);
    }
  }
  return (
    <Context.Provider
      value={{
        user,
        starting,
        loading,
        errors,
        pets,
        appointments,
        due,
        profile,
        selectedId: pet?.petId || "",
        selectPet,
        pet,
        details,
        detailLoading,
        detailErrors,
        reload,
        reloadDetails,
        signIn,
        signOut,
      }}
    >
      {children}
    </Context.Provider>
  );
}
export function useOwner() {
  const value = useContext(Context);
  if (!value) throw new Error("OwnerProvider is required");
  return value;
}
