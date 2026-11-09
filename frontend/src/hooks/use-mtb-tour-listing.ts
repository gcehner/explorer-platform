"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import {
  fetchMtbTours,
  type MtbBoundingBox,
  type MtbTourFilters,
  type MtbTourSummary,
  type TourListingStatus,
} from "@/lib/mtb-tour-listing";

interface ListingState {
  items: MtbTourSummary[];
  nextCursor: string | null;
  hasMore: boolean;
  status: TourListingStatus;
  loadingMore: boolean;
  loadMoreError: boolean;
}

const WAITING_STATE: ListingState = {
  items: [],
  nextCursor: null,
  hasMore: false,
  status: "waiting-for-map",
  loadingMore: false,
  loadMoreError: false,
};

export function useMtbTourListing(
  bounds: MtbBoundingBox | null | undefined,
  filters: MtbTourFilters,
) {
  const [state, setState] = useState<ListingState>(WAITING_STATE);
  const [retryGeneration, setRetryGeneration] = useState(0);
  const requestGeneration = useRef(0);
  const loadMoreInFlight = useRef(false);
  const loadMoreController = useRef<AbortController | null>(null);

  useEffect(() => {
    const generation = ++requestGeneration.current;
    loadMoreController.current?.abort();
    loadMoreController.current = null;
    loadMoreInFlight.current = false;

    if (bounds === undefined) {
      queueMicrotask(() => {
        if (generation === requestGeneration.current) setState(WAITING_STATE);
      });
      return;
    }
    if (bounds === null) {
      queueMicrotask(() => {
        if (generation === requestGeneration.current) {
          setState({ ...WAITING_STATE, status: "invalid-bounds" });
        }
      });
      return;
    }

    const controller = new AbortController();
    queueMicrotask(() => {
      if (generation !== requestGeneration.current) return;
      setState({
        items: [],
        nextCursor: null,
        hasMore: false,
        status: "loading",
        loadingMore: false,
        loadMoreError: false,
      });
    });

    void fetchMtbTours(bounds, filters, controller.signal)
      .then((page) => {
        if (generation !== requestGeneration.current) return;
        setState({
          items: page.items,
          nextCursor: page.nextCursor,
          hasMore: page.hasMore,
          status: "ready",
          loadingMore: false,
          loadMoreError: false,
        });
      })
      .catch(() => {
        if (controller.signal.aborted || generation !== requestGeneration.current) return;
        setState({
          items: [],
          nextCursor: null,
          hasMore: false,
          status: "error",
          loadingMore: false,
          loadMoreError: false,
        });
      });

    return () => controller.abort();
  }, [bounds, filters, retryGeneration]);

  const retry = useCallback(() => {
    setRetryGeneration((generation) => generation + 1);
  }, []);

  const loadMore = useCallback(() => {
    if (
      !bounds ||
      !state.hasMore ||
      !state.nextCursor ||
      loadMoreInFlight.current
    ) {
      return;
    }

    loadMoreInFlight.current = true;
    const generation = requestGeneration.current;
    const cursor = state.nextCursor;
    const controller = new AbortController();
    loadMoreController.current = controller;
    setState((current) => ({
      ...current,
      loadingMore: true,
      loadMoreError: false,
    }));

    void fetchMtbTours(bounds, filters, controller.signal, cursor)
      .then((page) => {
        if (controller.signal.aborted || generation !== requestGeneration.current) return;
        setState((current) => {
          const knownIds = new Set(current.items.map((tour) => tour.id));
          const newItems = page.items.filter((tour) => !knownIds.has(tour.id));
          return {
            ...current,
            items: [...current.items, ...newItems],
            nextCursor: page.nextCursor,
            hasMore: page.hasMore,
            loadingMore: false,
            loadMoreError: false,
          };
        });
      })
      .catch(() => {
        if (controller.signal.aborted || generation !== requestGeneration.current) return;
        setState((current) => ({
          ...current,
          loadingMore: false,
          loadMoreError: true,
        }));
      })
      .finally(() => {
        if (loadMoreController.current === controller) {
          loadMoreController.current = null;
          loadMoreInFlight.current = false;
        }
      });
  }, [bounds, filters, state.hasMore, state.nextCursor]);

  useEffect(
    () => () => {
      loadMoreController.current?.abort();
    },
    [],
  );

  return { ...state, retry, loadMore };
}
