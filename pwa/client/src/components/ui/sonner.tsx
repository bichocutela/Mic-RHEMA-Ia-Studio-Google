import { useTheme } from "next-themes";
import { Toaster as Sonner, type ToasterProps } from "sonner";
import "./sonner-pwa.css";

const Toaster = ({ ...props }: ToasterProps) => {
  const { theme = "system" } = useTheme();

  return (
    <Sonner
      theme={theme as ToasterProps["theme"]}
      className="toaster group"
      position="bottom-center"
      expand
      closeButton
      visibleToasts={3}
      offset={16}
      style={
        {
          "--normal-bg": "color-mix(in srgb, var(--popover) 94%, transparent)",
          "--normal-text": "var(--popover-foreground)",
          "--normal-border": "color-mix(in srgb, var(--border) 82%, transparent)",
        } as React.CSSProperties
      }
      {...props}
    />
  );
};

export { Toaster };
