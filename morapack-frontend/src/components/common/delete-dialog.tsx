"use client";

import { useState } from "react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Trash2, Loader2 } from "lucide-react";

interface DeleteDialogProps {
  title?: string;
  description?: string;
  itemName?: string;
  onConfirm: () => Promise<void>;
  trigger?: React.ReactNode;
  variant?: "icon" | "button";
}

export function DeleteDialog({
  title = "¿Estás seguro?",
  description = "Esta acción no se puede deshacer.",
  itemName,
  onConfirm,
  trigger,
  variant = "icon",
}: DeleteDialogProps) {
  const [open, setOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleConfirm = async () => {
    setIsDeleting(true);
    try {
      await onConfirm();
      setOpen(false);
    } catch (error) {
      console.error("Error al eliminar:", error);
    } finally {
      setIsDeleting(false);
    }
  };

  const defaultTrigger = variant === "icon" ? (
    <Button
      variant="ghost"
      size="icon"
      className="text-red-600 hover:text-red-700 hover:bg-red-50 h-8 w-8"
      title="Eliminar"
    >
      <Trash2 className="h-4 w-4" />
    </Button>
  ) : (
    <Button
      variant="destructive"
      size="sm"
      className="gap-2"
    >
      <Trash2 className="h-4 w-4" />
      Eliminar
    </Button>
  );

  return (
    <AlertDialog open={open} onOpenChange={setOpen}>
      <div onClick={() => setOpen(true)}>
        {trigger || defaultTrigger}
      </div>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>
            {itemName ? (
              <>
                {description}
                <br />
                <span className="font-semibold text-foreground mt-2 block">
                  {itemName}
                </span>
              </>
            ) : (
              description
            )}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isDeleting}>
            Cancelar
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleConfirm}
            disabled={isDeleting}
            className="bg-red-600 hover:bg-red-700 focus:ring-red-600"
          >
            {isDeleting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Eliminando...
              </>
            ) : (
              <>
                <Trash2 className="mr-2 h-4 w-4" />
                Eliminar
              </>
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
