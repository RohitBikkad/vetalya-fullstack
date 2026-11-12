import React from 'react';
import { Text,View } from 'react-native';
import { Redirect } from 'expo-router';
import { useOwner } from '../OwnerContext';
import { Screen,Card,styles,Icon } from '../ui';
import { PLANNED_SERVICES } from '../../../../packages/owner-domain/src/model.js';
export default function Services(){const {user}=useOwner();if(!user)return <Redirect href="/sign-in"/>;return <Screen title="Care beyond the clinic." subtitle="More ways to care, as connected services become available."><Card><Text style={styles.body}>Service-provider booking and insurance integrations are not connected yet. No orders, coverage or payments are created here.</Text></Card>{PLANNED_SERVICES.map(service=><Card key={service.name}><View style={styles.row}><Icon name="heart"/><Text style={styles.heading}>{service.name}</Text></View><Text style={styles.body}>{service.description}</Text><View style={styles.badge}><Text style={styles.badgeText}>Integration pending</Text></View></Card>)}</Screen>;}
