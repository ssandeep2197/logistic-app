import { type VariantProps } from 'class-variance-authority';
import { type ButtonHTMLAttributes } from 'react';
declare const buttonVariants: (props?: ({
    variant?: "primary" | "secondary" | "ghost" | "danger" | null | undefined;
    size?: "sm" | "md" | "lg" | null | undefined;
} & import("class-variance-authority/types").ClassProp) | undefined) => string;
export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {
}
export declare const Button: import("react").ForwardRefExoticComponent<ButtonProps & import("react").RefAttributes<HTMLButtonElement>>;
export {};
