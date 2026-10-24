package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.instance.ItemInstanceRegistry;
import com.virgolandstudio.items.core.registry.BuiltinSimulationSchemas;
import com.virgolandstudio.items.core.state.StateSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Provider-neutral fluid runtime for item instances.
 *
 * <p>Fluid identity/capacity remain authored component data while amount/type/temperature live in
 * persistent instance state. Transfers lock both instances in UUID order, validate the complete
 * operation first and only then mutate either side, so a failed transfer cannot duplicate or lose
 * resource.</p>
 */
public final class ItemFluidRuntimeService {
    private static final double EPSILON = 1.0e-9;

    private final ItemInstanceRegistry instances;

    public ItemFluidRuntimeService(ItemInstanceRegistry instances) {
        this.instances = Objects.requireNonNull(instances, "instances");
    }

    public ItemFluidTransferResult transfer(UUID sourceId, UUID destinationId, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0) {
            return ItemFluidTransferResult.failure(ItemFluidTransferResult.Status.INVALID_AMOUNT,
                    "Amount must be finite and > 0");
        }
        ItemInstance source = instances.find(sourceId).orElse(null);
        ItemInstance destination = instances.find(destinationId).orElse(null);
        if (source == null || destination == null) {
            return ItemFluidTransferResult.failure(ItemFluidTransferResult.Status.NOT_FOUND,
                    "Source or destination item instance was not found");
        }
        if (source == destination) {
            return ItemFluidTransferResult.failure(ItemFluidTransferResult.Status.NOT_SUPPORTED,
                    "Source and destination must be different item instances");
        }

        ItemInstance first = compare(source.id(), destination.id()) <= 0 ? source : destination;
        ItemInstance second = first == source ? destination : source;
        synchronized (first) {
            synchronized (second) {
                return transferLocked(source, destination, amount);
            }
        }
    }

    private static ItemFluidTransferResult transferLocked(ItemInstance source, ItemInstance destination, double amount) {
        FluidContract sourceContract = contract(source);
        FluidContract destinationContract = contract(destination);
        if (sourceContract == null || destinationContract == null) {
            return ItemFluidTransferResult.failure(ItemFluidTransferResult.Status.NOT_SUPPORTED,
                    "Both items require virgoland:fluid_storage");
        }

        StateSlot sourceAmountSlot = source.state().layout().find("fluid_amount");
        StateSlot sourceTypeSlot = source.state().layout().find("fluid_type");
        StateSlot destinationAmountSlot = destination.state().layout().find("fluid_amount");
        StateSlot destinationTypeSlot = destination.state().layout().find("fluid_type");
        if (sourceAmountSlot == null || sourceTypeSlot == null || destinationAmountSlot == null || destinationTypeSlot == null) {
            return ItemFluidTransferResult.failure(ItemFluidTransferResult.Status.NOT_SUPPORTED,
                    "Fluid state layout is unavailable");
        }

        double sourceAmount = source.state().getDouble(sourceAmountSlot, 0.0);
        if (sourceAmount <= EPSILON) {
            return ItemFluidTransferResult.failure(ItemFluidTransferResult.Status.EMPTY_SOURCE, "Source is empty");
        }
        if (sourceAmount + EPSILON < amount) {
            return new ItemFluidTransferResult(
                    ItemFluidTransferResult.Status.INSUFFICIENT,
                    fluidType(source, sourceTypeSlot, sourceContract.defaultFluid()),
                    0.0,
                    sourceAmount,
                    destination.state().getDouble(destinationAmountSlot, 0.0),
                    temperature(destination),
                    "Source does not contain enough fluid"
            );
        }

        String sourceFluid = fluidType(source, sourceTypeSlot, sourceContract.defaultFluid());
        if (sourceFluid.isBlank()) {
            return ItemFluidTransferResult.failure(ItemFluidTransferResult.Status.EMPTY_SOURCE,
                    "Source amount has no fluid identity");
        }
        if (!destinationContract.allowedFluids().isEmpty()
                && destinationContract.allowedFluids().stream().noneMatch(sourceFluid::equalsIgnoreCase)) {
            return ItemFluidTransferResult.failure(ItemFluidTransferResult.Status.FLUID_NOT_ALLOWED,
                    "Destination does not accept fluid " + sourceFluid);
        }

        double destinationAmount = destination.state().getDouble(destinationAmountSlot, 0.0);
        if (destinationAmount + amount > destinationContract.capacity() + EPSILON) {
            return new ItemFluidTransferResult(
                    ItemFluidTransferResult.Status.CAPACITY_EXCEEDED,
                    sourceFluid,
                    0.0,
                    sourceAmount,
                    destinationAmount,
                    temperature(destination),
                    "Destination capacity exceeded"
            );
        }

        String destinationFluid = fluidType(destination, destinationTypeSlot, destinationContract.defaultFluid());
        if (destinationAmount > EPSILON && !destinationFluid.isBlank() && !destinationFluid.equalsIgnoreCase(sourceFluid)) {
            if (!destinationContract.mixing()) {
                return ItemFluidTransferResult.failure(ItemFluidTransferResult.Status.MIXING_NOT_ALLOWED,
                        "Destination already contains " + destinationFluid);
            }
            return ItemFluidTransferResult.failure(ItemFluidTransferResult.Status.NOT_SUPPORTED,
                    "Mixed-fluid identities require a mixture resolver");
        }

        Double sourceTemperature = temperature(source);
        Double destinationTemperature = temperature(destination);
        Double mixedTemperature = destinationTemperature;
        if (sourceTemperature != null) {
            if (destinationTemperature == null || destinationAmount <= EPSILON) {
                mixedTemperature = sourceTemperature;
            } else {
                mixedTemperature = ((destinationTemperature * destinationAmount) + (sourceTemperature * amount))
                        / (destinationAmount + amount);
            }
        }

        double sourceRemaining = Math.max(0.0, sourceAmount - amount);
        double destinationAfter = destinationAmount + amount;
        source.state().set(sourceAmountSlot, sourceRemaining);
        destination.state().set(destinationAmountSlot, destinationAfter);
        if (destinationAmount <= EPSILON || destinationFluid.isBlank()) {
            destination.state().set(destinationTypeSlot, sourceFluid);
        }
        if (sourceRemaining <= EPSILON) source.state().set(sourceTypeSlot, "");
        if (mixedTemperature != null) setTemperature(destination, mixedTemperature);

        return new ItemFluidTransferResult(
                ItemFluidTransferResult.Status.APPLIED,
                sourceFluid,
                amount,
                sourceRemaining,
                destinationAfter,
                mixedTemperature,
                ""
        );
    }

    private static FluidContract contract(ItemInstance instance) {
        ComponentConfig component = instance.definition().source().components().stream()
                .filter(value -> value.type().equals(BuiltinSimulationSchemas.FLUID_STORAGE))
                .findFirst()
                .orElse(null);
        if (component == null) return null;
        double capacity = number(component, "capacity", 0.0);
        if (!Double.isFinite(capacity) || capacity < 0.0) return null;
        String defaultFluid = string(component, "fluid", "");
        boolean mixing = bool(component, "mixing", false);
        List<String> allowed = strings(component.values().get("allowedFluids"));
        return new FluidContract(capacity, defaultFluid, allowed, mixing);
    }

    private static Double temperature(ItemInstance instance) {
        StateSlot slot = instance.state().layout().find("temperature_c");
        return slot == null ? null : instance.state().getDouble(slot, 20.0);
    }

    private static void setTemperature(ItemInstance instance, double value) {
        StateSlot slot = instance.state().layout().find("temperature_c");
        if (slot != null) instance.state().set(slot, value);
    }

    private static String fluidType(ItemInstance instance, StateSlot slot, String fallback) {
        Object value = instance.state().get(slot);
        String type = value instanceof String text ? text : "";
        return type.isBlank() ? fallback : type;
    }

    private static List<String> strings(Object raw) {
        if (!(raw instanceof Iterable<?> iterable)) return List.of();
        var values = new ArrayList<String>();
        for (Object value : iterable) {
            if (value == null) continue;
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) values.add(text);
        }
        return List.copyOf(values);
    }

    private static double number(ComponentConfig component, String key, double fallback) {
        Object value = component.values().get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static String string(ComponentConfig component, String key, String fallback) {
        Object value = component.values().get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean bool(ComponentConfig component, String key, boolean fallback) {
        Object value = component.values().get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static int compare(UUID left, UUID right) {
        int msb = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
        return msb != 0 ? msb : Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
    }

    private record FluidContract(double capacity, String defaultFluid, List<String> allowedFluids, boolean mixing) {}
}
